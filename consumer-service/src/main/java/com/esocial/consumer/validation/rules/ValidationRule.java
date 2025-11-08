package com.esocial.consumer.validation.rules;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;

public interface ValidationRule {
    
    /**
     * Valida um evento de colaborador
     */
    void validate(EmployeeEventDTO event, ValidationResult result);
    
    /**
     * Nome da regra de validação
     */
    String getRuleName();
}
