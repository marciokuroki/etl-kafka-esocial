package com.esocial.consumer.validation.rules.structural;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class AdmissionDateValidationRule implements ValidationRule {
    @Override
    public String getRuleId() { return "VE-005"; }
    
    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        LocalDate admissionDate = event.getAdmissionDate();
        if (admissionDate == null) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "Data de admissão é obrigatória", 
                "admissionDate", null);
            return;
        }
        if (admissionDate.isAfter(LocalDate.now())) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "Data de admissão não pode ser futura", 
                "admissionDate", admissionDate.toString());
        }
    }
}
