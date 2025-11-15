package com.esocial.consumer.validation.rules.business;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class MaximumAgeValidationRule implements ValidationRule {

    private static final int MAXIMUM_AGE = 120;

    @Override
    public String getRuleId() {
        return "VN-002";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        LocalDate birthDate = event.getBirthDate();
        if (birthDate == null) return;

        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age > MAXIMUM_AGE) {
            result.addError(getRuleId(),
                    ValidationSeverity.WARNING,
                    "Colaborador não pode ter mais que 120 anos",
                    "birthDate",
                    birthDate.toString());
        }
    }
}
