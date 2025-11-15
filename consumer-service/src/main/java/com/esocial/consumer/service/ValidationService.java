package com.esocial.consumer.service;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.model.entity.ValidationError;
import com.esocial.consumer.repository.ValidationErrorRepository;
import com.esocial.consumer.validation.ValidationEngine;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationErrorDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ValidationService {
    
    private final ValidationEngine validationEngine;
    private final ValidationErrorRepository validationErrorRepository;
    private final ObjectMapper objectMapper;
    private final Counter validationSuccessCounter;
    private final Counter validationFailureCounter;
    
    public ValidationService(
            ValidationEngine validationEngine,
            ValidationErrorRepository validationErrorRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.validationEngine = validationEngine;
        this.validationErrorRepository = validationErrorRepository;
        this.objectMapper = objectMapper;
        this.validationSuccessCounter = Counter.builder("validation.success")
                .description("Total de validações bem-sucedidas")
                .tag("service", "consumer")
                .register(meterRegistry);
        this.validationFailureCounter = Counter.builder("validation.failure")
                .description("Total de validações falhadas")
                .tag("service", "consumer")
                .register(meterRegistry);
    }
    
    /**
     * Valida um evento e persiste erros e warnings encontrados.
     * Registra métrica de sucesso ou falha.
     */
    @Transactional
    public ValidationResult validateAndPersistErrors(EmployeeEventDTO event, 
                                                     Long kafkaOffset, 
                                                     Integer kafkaPartition, 
                                                     String kafkaTopic) {
        
        ValidationResult result = validationEngine.validate(event);
        
        if (result.hasErrors()) {
            validationFailureCounter.increment();
            persistValidationErrors(event, result, kafkaOffset, kafkaPartition, kafkaTopic);
        } else {
            validationSuccessCounter.increment();
        }
        
        if (result.hasWarnings()) {
            persistValidationWarnings(event, result, kafkaOffset, kafkaPartition, kafkaTopic);
        }
        
        return result;
    }
    
    private void persistValidationErrors(EmployeeEventDTO event, 
                                        ValidationResult result, 
                                        Long kafkaOffset, 
                                        Integer kafkaPartition, 
                                        String kafkaTopic) {
        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            
            for (ValidationErrorDTO error : result.getErrors()) {
                ValidationError entity = ValidationError.builder()
                        .eventId(event.getEventId())
                        .sourceTable("employees")
                        .sourceId(event.getSourceId())
                        .validationRule(error.getRuleId())
                        .errorMessage(error.getMessage())
                        .severity(error.getSeverity().name())
                        .fieldName(error.getField())
                        .fieldValue(error.getValue() != null ? error.getValue().toString() : null)
                        .eventPayload(eventPayload)
                        .kafkaOffset(kafkaOffset)
                        .kafkaPartition(kafkaPartition)
                        .kafkaTopic(kafkaTopic)
                        .correlationId(event.getCorrelationId())
                        .build();
                
                validationErrorRepository.save(entity);
            }
            
            log.info("Persistidos {} erros de validação para evento {}", 
                    result.getErrors().size(), event.getEventId());
        } catch (Exception e) {
            log.error("Erro ao persistir erros de validação: {}", e.getMessage(), e);
        }
    }
    
    private void persistValidationWarnings(EmployeeEventDTO event, 
                                          ValidationResult result, 
                                          Long kafkaOffset, 
                                          Integer kafkaPartition, 
                                          String kafkaTopic) {
        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            
            for (ValidationErrorDTO warning : result.getWarnings()) {
                ValidationError entity = ValidationError.builder()
                        .eventId(event.getEventId())
                        .sourceTable("employees")
                        .sourceId(event.getSourceId())
                        .validationRule(warning.getRuleId())
                        .errorMessage(warning.getMessage())
                        .severity(warning.getSeverity().name())
                        .fieldName(warning.getField())
                        .fieldValue(warning.getValue() != null ? warning.getValue().toString() : null)
                        .eventPayload(eventPayload)
                        .kafkaOffset(kafkaOffset)
                        .kafkaPartition(kafkaPartition)
                        .kafkaTopic(kafkaTopic)
                        .correlationId(event.getCorrelationId())
                        .build();
                
                validationErrorRepository.save(entity);
            }
            
            log.debug("Persistidos {} warnings para evento {}", 
                    result.getWarnings().size(), event.getEventId());
        } catch (Exception e) {
            log.error("Erro ao persistir warnings: {}", e.getMessage(), e);
        }
    }
}
