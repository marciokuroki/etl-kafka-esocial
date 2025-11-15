package com.esocial.consumer.validation.rules.conformance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class MaritalStatusValidationRule implements ValidationRule {

    private static final Set<String> VALID_STATUSES = new HashSet<>(
            Arrays.asList("1", "2", "3", "4", "5")
    );

    @Override
    public String getRuleId() {
        return "VC-006";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String maritalStatus = event.getMaritalStatus();
        
        if (maritalStatus != null && !maritalStatus.isEmpty()) {
            if (!VALID_STATUSES.contains(maritalStatus)) {
                result.addError(getRuleId(), ValidationSeverity.WARNING, 
                    "Estado civil deve estar conforme tabela eSocial (1-5)", 
                    "maritalStatus", maritalStatus);
            }
        }
    }
}
