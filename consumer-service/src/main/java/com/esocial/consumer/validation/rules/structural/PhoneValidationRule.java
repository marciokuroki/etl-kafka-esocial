package com.esocial.consumer.validation.rules.structural;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class PhoneValidationRule implements ValidationRule {
    @Override
    public String getRuleId() { return "VE-008"; }
    
    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String phone = event.getPhone();
        if (phone != null && !phone.isEmpty()) {
            String digitsOnly = phone.replaceAll("\\D", "");
            if (digitsOnly.length() < 10 || digitsOnly.length() > 11) {
                result.addError(getRuleId(), ValidationSeverity.WARNING, "Telefone deve conter 10 ou 11 dígitos", 
                    "phone", phone);
            }
        }
    }
}
