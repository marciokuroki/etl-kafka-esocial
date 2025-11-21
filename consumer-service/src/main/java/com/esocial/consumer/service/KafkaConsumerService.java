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
            Acknowledgment acknowledgment) {
                
        eventsConsumedCounter.increment();
        
        consumerMetrics.incrementConsumedEvents(
            event.getEventType(),  
            topic
        );
        
        //  Registrar tamanho do payload (Sprint 3)
        int payloadSize = estimatePayloadSize(event);
        consumerMetrics.recordPayloadSize(payloadSize);
        
        log.info("Evento consumido: eventId={}, type={}, topic={}, partition={}, offset={}, size={}bytes", 
                event.getEventId(), event.getEventType(), topic, partition, offset, payloadSize);
        
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
        
        //  Medir tempo de validação (Sprint 3)
        Timer.Sample validationSample = consumerMetrics.startValidation();
        
        // 1. Validar evento
        ValidationResult validationResult = validationService.validateAndPersistErrors(
                event, offset, partition, topic);
        
        //  Parar timer de validação (Sprint 3)
        consumerMetrics.stopValidation(validationSample);
        
        // 2. Se válido, persistir no banco
        if (validationResult.isValid()) {
            // Métrica de validação bem-sucedida 
            consumerMetrics.incrementValidationSuccess(event.getEventType()); 
            
            //  Medir tempo de persistência (Sprint 3)
            Timer.Sample persistenceSample = consumerMetrics.startPersistence();
            
            persistenceService.persistEvent(event, offset, partition, topic);
            
            //  Parar timer de persistência (Sprint 3)
            consumerMetrics.stopPersistence(persistenceSample);
            
            log.debug("Evento persistido com sucesso: {}", event.getEventId());
        } else {
            //  Métrica de validação falha (Sprint 3)
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
            if (sb.length() > 5000) break; // Limitar tamanho
        }
        return sb.toString();
    }
    
    /**
     * Estima tamanho do payload baseado nos campos reais do DTO
     */
    private int estimatePayloadSize(EmployeeEventDTO event) {
        int baseSize = 300; // Campos fixos: UUIDs, datas, números
        
        // Documentos
        if (event.getSourceId() != null) baseSize += event.getSourceId().length() * 2;
        if (event.getEventId() != null) baseSize += event.getEventId().length() * 2;
        if (event.getCpf() != null) baseSize += event.getCpf().length() * 2;
        if (event.getPis() != null) baseSize += event.getPis().length() * 2;
        if (event.getCtps() != null) baseSize += event.getCtps().length() * 2;
        if (event.getMatricula() != null) baseSize += event.getMatricula().length() * 2;
        
        // Dados Pessoais
        if (event.getFullName() != null) baseSize += event.getFullName().length() * 2;
        if (event.getSex() != null) baseSize += event.getSex().length() * 2;
        if (event.getNationality() != null) baseSize += event.getNationality().length() * 2;
        if (event.getMaritalStatus() != null) baseSize += event.getMaritalStatus().length() * 2;
        if (event.getRace() != null) baseSize += event.getRace().length() * 2;
        if (event.getEducationLevel() != null) baseSize += event.getEducationLevel().length() * 2;
        if (event.getDisability() != null) baseSize += event.getDisability().length() * 2;
        
        // Contato
        if (event.getEmail() != null) baseSize += event.getEmail().length() * 2;
        if (event.getPhone() != null) baseSize += event.getPhone().length() * 2;
        if (event.getZipCode() != null) baseSize += event.getZipCode().length() * 2;
        if (event.getUf() != null) baseSize += event.getUf().length() * 2;
        
        // Laborais
        if (event.getJobTitle() != null) baseSize += event.getJobTitle().length() * 2;
        if (event.getDepartment() != null) baseSize += event.getDepartment().length() * 2;
        if (event.getCategory() != null) baseSize += event.getCategory().length() * 2;
        if (event.getContractType() != null) baseSize += event.getContractType().length() * 2;
        if (event.getCbo() != null) baseSize += event.getCbo().length() * 2;
        
        // Status e Metadata
        if (event.getStatus() != null) baseSize += event.getStatus().length() * 2;
        if (event.getEventType() != null) baseSize += event.getEventType().length() * 2;
        if (event.getKafkaTopic() != null) baseSize += event.getKafkaTopic().length() * 2;
        
        return baseSize;
    }
}
