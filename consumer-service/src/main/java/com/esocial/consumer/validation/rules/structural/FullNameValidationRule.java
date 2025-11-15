package com.esocial.consumer.validation.rules.structural;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class FullNameValidationRule implements ValidationRule {
    @Override
    public String getRuleId() { return "VE-003"; }
    
    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String name = event.getFullName();
        if (name == null || name.trim().isEmpty()) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "Nome é obrigatório", 
                "fullName", name);
            return;
        }
        if (name.length() < 3 || name.length() > 200) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "Nome deve ter entre 3 e 200 caracteres", 
                "fullName", name);
            return;
        }
        if (name.matches("\\s+")) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "Nome não pode conter apenas espaços", 
                "fullName", name);
        }
    }
}

