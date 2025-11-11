package com.esocial.consumer.validation;

import com.esocial.consumer.model.dto.EmployeeEventDTO;

public interface ValidationRule {
    String getRuleId();
    //ValidationSeverity getSeverity();
    void validate(EmployeeEventDTO event, ValidationResult result);
}
