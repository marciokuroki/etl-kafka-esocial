package com.esocial.consumer.validation.rules.structural;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class PisValidationRule implements ValidationRule {
    @Override
    public String getRuleId() { return "VE-002"; }
    
    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String pis = event.getPis();
        if (pis != null && !pis.matches("\\d{11}")) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "PIS deve conter 11 dígitos numéricos", 
                "pis", pis);
        }
    }
}
