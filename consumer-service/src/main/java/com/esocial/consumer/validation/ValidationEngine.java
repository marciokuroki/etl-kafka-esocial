package com.esocial.consumer.validation;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import java.util.List;

public class ValidationEngine {
    private final List<ValidationRule> rules;

    public ValidationEngine(List<ValidationRule> rules) {
        this.rules = rules;
    }

    public ValidationResult validate(EmployeeEventDTO event) {
        ValidationResult result = new ValidationResult();
        for (ValidationRule rule : rules) {
            try {
                rule.validate(event, result);
            } catch (Exception ex) {
                result.addError(
                    rule.getRuleId(),
                    ValidationSeverity.ERROR,
                    "Erro interno na validação: " + ex.getMessage(),
                    null,
                    null
                );
            }
        }
        return result;
    }
}
