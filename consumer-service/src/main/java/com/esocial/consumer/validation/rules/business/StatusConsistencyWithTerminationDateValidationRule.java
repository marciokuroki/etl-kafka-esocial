package com.esocial.consumer.validation.rules.business;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class StatusConsistencyWithTerminationDateValidationRule implements ValidationRule {

    @Override
    public String getRuleId() {
        return "VN-005";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String status = event.getStatus();
        LocalDate terminationDate = event.getTerminationDate();

        if ("ACTIVE".equals(status) && terminationDate != null) {
            result.addError(getRuleId(),
                    ValidationSeverity.ERROR,
                    "Colaborador ativo não pode ter data de desligamento",
                    "status",
                    status);
        }

        if ("INACTIVE".equals(status) && terminationDate == null) {
            result.addError(getRuleId(),
                    ValidationSeverity.WARNING,
                    "Colaborador inativo deveria ter data de desligamento",
                    "status",
                    status);
        }
    }
}
