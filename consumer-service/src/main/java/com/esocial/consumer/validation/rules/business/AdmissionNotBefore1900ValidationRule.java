package com.esocial.consumer.validation.rules.business;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class AdmissionNotBefore1900ValidationRule implements ValidationRule {

    private static final LocalDate MINIMUM_DATE = LocalDate.of(1900, 1, 1);

    @Override
    public String getRuleId() {
        return "VN-003";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        LocalDate admissionDate = event.getAdmissionDate();
        if (admissionDate == null) return;

        if (admissionDate.isBefore(MINIMUM_DATE)) {
            result.addError(getRuleId(),
                    ValidationSeverity.ERROR,
                    "Data de admissão não pode ser anterior a 01/01/1900",
                    "admissionDate",
                    admissionDate.toString());
        }
    }
}
