package com.esocial.consumer.service;

import com.esocial.consumer.metrics.ConsumerMetrics;
import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.model.entity.DlqEvent;
import com.esocial.consumer.repository.DlqEventRepository;
import com.esocial.consumer.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class KafkaConsumerService {

    private final ValidationService validationService;
    private final PersistenceService persistenceService;
    private final DlqEventRepository dlqEventRepository;
    private final ObjectMapper objectMapper;
    private final Counter eventsConsumedCounter;
    private final Counter eventsProcessedCounter;
    private final Counter eventsFailedCounter;
    private final Timer processingTimer;
    private final ConsumerMetrics consumerMetrics;

    public KafkaConsumerService(
            ValidationService validationService,
            PersistenceService persistenceService,
            DlqEventRepository dlqEventRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            ConsumerMetrics consumerMetrics) {
        this.validationService = validationService;
        this.persistenceService = persistenceService;
        this.dlqEventRepository = dlqEventRepository;
        this.objectMapper = objectMapper;
        this.consumerMetrics = consumerMetrics;

        this.eventsConsumedCounter = Counter.builder("events.consumed")
                .description("Total de eventos consumidos do Kafka")
                .tag("service", "consumer")
                .register(meterRegistry);

        this.eventsProcessedCounter = Counter.builder("events.processed")
                .description("Total de eventos processados com sucesso")
                .tag("service", "consumer")
                .register(meterRegistry);

        this.eventsFailedCounter = Counter.builder("events.failed")
                .description("Total de eventos que falharam no processamento")
                .tag("service", "consumer")
                .register(meterRegistry);

        this.processingTimer = Timer.builder("events.processing.time")
                .description("Tempo de processamento de eventos")
                .tag("service", "consumer")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = {"${app.kafka.topics.employee-create}",
                      "${app.kafka.topics.employee-update}",
                      "${app.kafka.topics.employee-delete}"},
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEmployeeEvent(
            @Payload EmployeeEventDTO event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header("kafka_receivedMessageHeaders") Headers headers,
            Acknowledgment acknowledgment) {

        UUID correlationId = extractCorrelationIdAsUUID(headers);
        if (correlationId == null) {
            correlationId = UUID.randomUUID();
        }

        MDC.put("correlationId", correlationId.toString());

        try {
            eventsConsumedCounter.increment();

            consumerMetrics.incrementConsumedEvents(
                    event.getEventType(),
                    topic
            );

            int payloadSize = estimatePayloadSize(event);
            consumerMetrics.recordPayloadSize(payloadSize);

            log.info("Evento consumido: eventId={}, type={}, topic={}, partition={}, offset={}, size={}bytes, correlationId={}",
                    event.getEventId(), event.getEventType(), topic, partition, offset, payloadSize, correlationId);

            processingTimer.record(() -> {
                try {
                    processEvent(event, topic, offset, partition);

                    eventsProcessedCounter.increment();

                    acknowledgment.acknowledge();

                    log.info("Evento processado com sucesso: eventId={}", event.getEventId());

                } catch (Exception e) {
                    eventsFailedCounter.increment();

                    log.error("Erro ao processar evento {}: {}", event.getEventId(), e.getMessage(), e);

                    sendToDLQ(event, topic, offset, partition, e);

                    acknowledgment.acknowledge();
                }
            });
        } finally {
            MDC.remove("correlationId");
        }
    }

    private UUID extractCorrelationIdAsUUID(Headers headers) {
    return Optional.ofNullable(headers.lastHeader("X-Correlation-Id"))
            .map(header -> {
                String cidString = new String(header.value(), StandardCharsets.UTF_8);
                return UUID.fromString(cidString);
            })
            .orElse(null);
    }

    private void processEvent(EmployeeEventDTO event, String topic, Long offset, Integer partition) {
        log.debug("Iniciando processamento do evento: {}", event.getEventId());

        Timer.Sample validationSample = consumerMetrics.startValidation();

        ValidationResult validationResult = validationService.validateAndPersistErrors(
                event, offset, partition, topic);

        consumerMetrics.stopValidation(validationSample);

        if (validationResult.isValid()) {
            consumerMetrics.incrementValidationSuccess(event.getEventType());

            Timer.Sample persistenceSample = consumerMetrics.startPersistence();

            persistenceService.persistEvent(event, offset, partition, topic);

            consumerMetrics.stopPersistence(persistenceSample);

            log.debug("Evento persistido com sucesso: {}", event.getEventId());
        } else {
            String severity = validationResult.hasErrors() ? "ERROR" : "WARNING";
            consumerMetrics.incrementValidationFailure(event.getEventType(), severity);

            log.warn("Evento {} falhou na validação. Total de erros: {}",
                    event.getEventId(), validationResult.getErrors().size());
            throw new RuntimeException("Falha na validação: " +
                    validationResult.getErrors().size() + " erros encontrados");
        }
    }

    private void sendToDLQ(EmployeeEventDTO event, String topic, Long offset,
                           Integer partition, Exception exception) {
        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            String stackTrace = getStackTrace(exception);

            DlqEvent dlqEvent = DlqEvent.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .sourceTable("employees")
                    .sourceId(event.getSourceId())
                    .eventPayload(eventPayload)
                    .errorMessage(exception.getMessage())
                    .stackTrace(stackTrace)
                    .kafkaOffset(offset)
                    .kafkaPartition(partition)
                    .kafkaTopic(topic)
                    .correlationId(event.getCorrelationId())
                    .build();

            dlqEventRepository.save(dlqEvent);

            log.info("Evento enviado para DLQ: eventId={}, dlqId={}",
                    event.getEventId(), dlqEvent.getId());

        } catch (Exception e) {
            log.error("Erro ao enviar evento para DLQ: {}", e.getMessage(), e);
        }
    }

    private String getStackTrace(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getMessage()).append("\n");
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 5000) break;
        }
        return sb.toString();
    }

    private int estimatePayloadSize(EmployeeEventDTO event) {
        int baseSize = 300;

        if (event.getSourceId() != null) baseSize += event.getSourceId().length() * 2;
        if (event.getEventId() != null) baseSize += event.getEventId().length() * 2;
        if (event.getCpf() != null) baseSize += event.getCpf().length() * 2;
        if (event.getPis() != null) baseSize += event.getPis().length() * 2;
        if (event.getCtps() != null) baseSize += event.getCtps().length() * 2;
        if (event.getMatricula() != null) baseSize += event.getMatricula().length() * 2;

        if (event.getFullName() != null) baseSize += event.getFullName().length() * 2;
        if (event.getSex() != null) baseSize += event.getSex().length() * 2;
        if (event.getNationality() != null) baseSize += event.getNationality().length() * 2;
        if (event.getMaritalStatus() != null) baseSize += event.getMaritalStatus().length() * 2;
        if (event.getRace() != null) baseSize += event.getRace().length() * 2;
        if (event.getEducationLevel() != null) baseSize += event.getEducationLevel().length() * 2;
        if (event.getDisability() != null) baseSize += event.getDisability().length() * 2;

        if (event.getEmail() != null) baseSize += event.getEmail().length() * 2;
        if (event.getPhone() != null) baseSize += event.getPhone().length() * 2;
        if (event.getZipCode() != null) baseSize += event.getZipCode().length() * 2;
        if (event.getUf() != null) baseSize += event.getUf().length() * 2;

        if (event.getJobTitle() != null) baseSize += event.getJobTitle().length() * 2;
        if (event.getDepartment() != null) baseSize += event.getDepartment().length() * 2;
        if (event.getCategory() != null) baseSize += event.getCategory().length() * 2;
        if (event.getContractType() != null) baseSize += event.getContractType().length() * 2;
        if (event.getCbo() != null) baseSize += event.getCbo().length() * 2;

        if (event.getStatus() != null) baseSize += event.getStatus().length() * 2;
        if (event.getEventType() != null) baseSize += event.getEventType().length() * 2;
        if (event.getKafkaTopic() != null) baseSize += event.getKafkaTopic().length() * 2;

        return baseSize;
    }
}
