package com.esocial.consumer.validation.rules.conformance;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class CboValidationRule implements ValidationRule {

    @Override
    public String getRuleId() {
        return "VC-003";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String cbo = event.getCbo();
        
        if (cbo != null && !cbo.isEmpty()) {
            // CBO deve ter 6 dígitos numéricos (CBO 2002)
            if (!cbo.matches("\\d{6}")) {
                result.addError(getRuleId(), ValidationSeverity.WARNING, 
                    "CBO deve conter 6 dígitos conforme tabela CBO 2002", 
                    "cbo", cbo);
            }
        }
    }
}
