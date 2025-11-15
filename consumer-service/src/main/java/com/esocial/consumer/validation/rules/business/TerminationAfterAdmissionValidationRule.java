package com.esocial.consumer.validation.rules.business;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class TerminationAfterAdmissionValidationRule implements ValidationRule {

    @Override
    public String getRuleId() {
        return "VN-004";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        LocalDate admissionDate = event.getAdmissionDate();
        LocalDate terminationDate = event.getTerminationDate();

        if (admissionDate != null && terminationDate != null) {
            if (terminationDate.isBefore(admissionDate)) {
                result.addError(getRuleId(),
                        ValidationSeverity.ERROR,
                        "Data de desligamento deve ser posterior à data de admissão",
                        "terminationDate",
                        terminationDate.toString());
            }
        }
    }
}
