package com.esocial.consumer.service;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.model.entity.DlqEvent;
import com.esocial.consumer.repository.DlqEventRepository;
import com.esocial.consumer.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

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
    
    public KafkaConsumerService(
            ValidationService validationService,
            PersistenceService persistenceService,
            DlqEventRepository dlqEventRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.validationService = validationService;
        this.persistenceService = persistenceService;
        this.dlqEventRepository = dlqEventRepository;
        this.objectMapper = objectMapper;
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
            Acknowledgment acknowledgment) {
        
        eventsConsumedCounter.increment();
        
        log.info("Evento consumido: eventId={}, type={}, topic={}, partition={}, offset={}", 
                event.getEventId(), event.getEventType(), topic, partition, offset);
        
        processingTimer.record(() -> {
            try {
                processEvent(event, topic, offset, partition);
                eventsProcessedCounter.increment();
                
                // Commitar offset manualmente após sucesso
                acknowledgment.acknowledge();
                
                log.info("Evento processado com sucesso: eventId={}", event.getEventId());
                
            } catch (Exception e) {
                eventsFailedCounter.increment();
                log.error("Erro ao processar evento {}: {}", event.getEventId(), e.getMessage(), e);
                
                // Enviar para DLQ
                sendToDLQ(event, topic, offset, partition, e);
                
                // Commitar mesmo em caso de erro (após enviar para DLQ)
                acknowledgment.acknowledge();
            }
        });
    }
    
    private void processEvent(EmployeeEventDTO event, String topic, Long offset, Integer partition) {
        log.debug("Iniciando processamento do evento: {}", event.getEventId());
        
        // 1. Validar evento
        ValidationResult validationResult = validationService.validateAndPersistErrors(
                event, offset, partition, topic);
        
        // 2. Se válido, persistir no banco
        if (validationResult.isValid()) {
            persistenceService.persistEvent(event, offset, partition, topic);
            log.debug("Evento persistido com sucesso: {}", event.getEventId());
        } else {
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
            if (sb.length() > 5000) break; // Limitar tamanho
        }
        return sb.toString();
    }
}
