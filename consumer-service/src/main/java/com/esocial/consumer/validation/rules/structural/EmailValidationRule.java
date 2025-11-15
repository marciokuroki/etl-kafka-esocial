package com.esocial.consumer.validation.rules.structural;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class EmailValidationRule implements ValidationRule {
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    
    @Override
    public String getRuleId() { return "VE-007"; }
    
    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String email = event.getEmail();
        if (email != null && !email.isEmpty() && !email.matches(EMAIL_PATTERN)) {
            result.addError(getRuleId(), ValidationSeverity.WARNING, "Email em formato inválido", 
                "email", email);
        }
    }
}
