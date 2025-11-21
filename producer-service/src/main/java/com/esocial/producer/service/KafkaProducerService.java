package com.esocial.producer.service;

import com.esocial.producer.model.dto.EmployeeEventDTO;
import com.esocial.producer.model.dto.EventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    
    private final Counter eventsPublishedCounter;
    private final Counter eventsFailedCounter;    
    private final Timer kafkaPublishTimer;
    private final DistributionSummary payloadSizeDistribution;

    @Value("${app.kafka.topics.employee-create}")
    private String employeeCreateTopic;

    @Value("${app.kafka.topics.employee-update}")
    private String employeeUpdateTopic;

    @Value("${app.kafka.topics.employee-delete}")
    private String employeeDeleteTopic;

    public KafkaProducerService(
            KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        
        this.eventsPublishedCounter = Counter.builder("events.published")
                .description("Total de eventos publicados no Kafka")
                .tag("service", "producer")
                .register(meterRegistry);
        
        this.eventsFailedCounter = Counter.builder("events.failed")
                .description("Total de eventos que falharam ao publicar")
                .tag("service", "producer")
                .register(meterRegistry);
        
        this.kafkaPublishTimer = Timer.builder("kafka.publish.duration")
                .description("Tempo de publicação de evento no Kafka")
                .tag("service", "producer")
                .publishPercentiles(0.5, 0.95, 0.99) // P50, P95, P99
                .register(meterRegistry);
                
        this.payloadSizeDistribution = DistributionSummary.builder("events.payload.size")
                .description("Tamanho dos payloads publicados em bytes")
                .tag("service", "producer")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    /**
     * Publica evento de colaborador no Kafka
     */
    public void publishEmployeeEvent(EmployeeEventDTO event) {
        String topic = determineTopicByEventType(event.getEventType());
        String key = event.getEmployeeId();

        log.debug("Publicando evento no tópico {}: eventId={}, employeeId={}, type={}",
                topic, event.getEventId(), event.getEmployeeId(), event.getEventType());
        
        Timer.Sample sample = Timer.start(meterRegistry);
                
        // Registrar tamanho real do payload
        int payloadSize = calculateActualPayloadSize(event);
        payloadSizeDistribution.record(payloadSize);

        CompletableFuture<SendResult<String, EmployeeEventDTO>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {            
            long latencyMs = (long) sample.stop(kafkaPublishTimer);
            
            if (ex == null) {                
                eventsPublishedCounter.increment();
                
                Counter.builder("events.published.total")
                        .description("Total de eventos publicados (detalhado)")
                        .tag("service", "producer")
                        .tag("event_type", event.getEventType().name())
                        .tag("topic", topic)
                        .register(meterRegistry)
                        .increment();
                
                log.info("Evento publicado com sucesso: topic={}, partition={}, offset={}, eventId={}, latency={}ms, size={}bytes",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getEventId(),
                        latencyMs,
                        payloadSize);
            } else {                
                eventsFailedCounter.increment();
                                
                Counter.builder("events.failed.total")
                        .description("Total de eventos que falharam (detalhado)")
                        .tag("service", "producer")
                        .tag("event_type", event.getEventType().name())
                        .tag("topic", topic)
                        .tag("error_type", ex.getClass().getSimpleName())
                        .register(meterRegistry)
                        .increment();
                
                log.error("Erro ao publicar evento: eventId={}, error={}",
                        event.getEventId(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * Determina o tópico baseado no tipo de evento
     */
    private String determineTopicByEventType(EventType eventType) {
        return switch (eventType) {
            case CREATE -> employeeCreateTopic;
            case UPDATE -> employeeUpdateTopic;
            case DELETE -> employeeDeleteTopic;
        };
    }
    
    /**
     * Cálculo preciso do tamanho real do JSON
     * Serializa o objeto e mede o tamanho em bytes
     */
    private int calculateActualPayloadSize(EmployeeEventDTO event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            return json.getBytes().length;
        } catch (JsonProcessingException e) {
            log.warn("Erro ao calcular tamanho do payload, usando estimativa: {}", e.getMessage());
            return estimatePayloadSize(event);
        }
    }


    private int estimatePayloadSize(EmployeeEventDTO event) {
        int baseSize = 250; // Campos fixos: IDs (UUIDs), datas, enums, BigDecimal
        
        // Adicionar tamanho dos campos String variáveis
        // Java String usa UTF-16: 2 bytes por caractere
        
        if (event.getEmployeeId() != null) {
            baseSize += event.getEmployeeId().length() * 2;
        }
        
        if (event.getCpf() != null) {
            baseSize += event.getCpf().length() * 2; // 11 chars = 22 bytes
        }
        
        if (event.getPis() != null) {
            baseSize += event.getPis().length() * 2; // 11 chars = 22 bytes
        }
        
        if (event.getFullName() != null) {
            baseSize += event.getFullName().length() * 2; // Nome completo variável
        }
        
        if (event.getJobTitle() != null) {
            baseSize += event.getJobTitle().length() * 2; // Cargo variável
        }
        
        if (event.getDepartment() != null) {
            baseSize += event.getDepartment().length() * 2; // Departamento variável
        }
        
        if (event.getStatus() != null) {
            baseSize += event.getStatus().length() * 2; // Status: ACTIVE, INACTIVE, etc
        }
        
        if (event.getSourceSystem() != null) {
            baseSize += event.getSourceSystem().length() * 2; // HR_SYSTEM, etc
        }
        
        return baseSize;
    }
}
