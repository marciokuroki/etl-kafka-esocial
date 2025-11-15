package com.esocial.consumer.validation.rules.business;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationSeverity;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.Period;

@Component
public class AgeAtAdmissionValidationRule implements ValidationRule {

    private static final int MINIMUM_AGE = 16;

    @Override
    public String getRuleId() {
        return "VN-001";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        LocalDate birthDate = event.getBirthDate();
        LocalDate admissionDate = event.getAdmissionDate();

        if (birthDate == null || admissionDate == null) {
            return;
        }

        int age = Period.between(birthDate, admissionDate).getYears();
        if (age < MINIMUM_AGE) {
            result.addError(getRuleId(),
                    ValidationSeverity.ERROR,
                    "Colaborador deve ter pelo menos 16 anos na data de admissão",
                    "birthDate,admissionDate",
                    String.format("%s/%s", birthDate, admissionDate));
        }
    }
}
