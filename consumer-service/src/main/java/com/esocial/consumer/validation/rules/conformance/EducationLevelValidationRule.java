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
public class EducationLevelValidationRule implements ValidationRule {

    private static final Set<String> VALID_LEVELS = new HashSet<>(
            Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08")
    );

    @Override
    public String getRuleId() {
        return "VC-008";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String educationLevel = event.getEducationLevel();
        
        if (educationLevel != null && !educationLevel.isEmpty()) {
            if (!VALID_LEVELS.contains(educationLevel)) {
                result.addError(getRuleId(), ValidationSeverity.WARNING, 
                    "Nível de educação deve estar conforme tabela eSocial", 
                    "educationLevel", educationLevel);
            }
        }
    }
}
