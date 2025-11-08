package com.esocial.consumer.validation;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.rules.ValidationRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ValidationEngine {
    
    private final List<ValidationRule> validationRules;
    
    public ValidationEngine(List<ValidationRule> validationRules) {
        this.validationRules = validationRules;
        log.info("ValidationEngine inicializado com {} regras", validationRules.size());
    }
    
    /**
     * Executa todas as regras de validação sobre um evento
     */
    public ValidationResult validate(EmployeeEventDTO event) {
        log.debug("Iniciando validação para evento: {} (tipo: {})", 
                event.getEventId(), event.getEventType());
        
        ValidationResult result = ValidationResult.builder()
                .valid(true)
                .build();
        
        // Executar todas as regras de validação
        for (ValidationRule rule : validationRules) {
            try {
                rule.validate(event, result);
            } catch (Exception e) {
                log.error("Erro ao executar regra de validação {}: {}", 
                        rule.getRuleName(), e.getMessage(), e);
                result.addError(rule.getRuleName(), 
                        "Erro interno na validação: " + e.getMessage(), null, null);
            }
        }
        
        log.debug("Validação concluída. Válido: {}, Erros: {}, Warnings: {}", 
                result.isValid(), result.getErrors().size(), result.getWarnings().size());
        
        return result;
    }
}
