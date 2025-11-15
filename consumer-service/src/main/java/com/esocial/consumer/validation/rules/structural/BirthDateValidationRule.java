package com.esocial.consumer.validation.rules.structural;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class BirthDateValidationRule implements ValidationRule {
    @Override
    public String getRuleId() { return "VE-004"; }
    
    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        LocalDate birthDate = event.getBirthDate();
        if (birthDate == null) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "Data de nascimento é obrigatória", 
                "birthDate", null);
            return;
        }
        if (birthDate.isAfter(LocalDate.now())) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "Data de nascimento não pode ser futura", 
                "birthDate", birthDate.toString());
        }
    }
}
