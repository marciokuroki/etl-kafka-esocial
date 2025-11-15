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
public class WorkerDisabilityValidationRule implements ValidationRule {

    private static final Set<String> VALID_DISABILITIES = new HashSet<>(
            Arrays.asList("01", "02", "03", "04", "05", "06", "07")
    );

    @Override
    public String getRuleId() {
        return "VC-010";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String disability = event.getDisability();
        
        if (disability != null && !disability.isEmpty()) {
            if (!VALID_DISABILITIES.contains(disability)) {
                result.addError(getRuleId(), ValidationSeverity.WARNING, 
                    "Tipo de deficiência deve estar conforme tabela eSocial", 
                    "disability", disability);
            }
        }
    }
}
