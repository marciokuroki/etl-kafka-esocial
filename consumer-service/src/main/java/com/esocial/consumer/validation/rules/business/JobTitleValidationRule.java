package com.esocial.consumer.validation.rules.business;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class JobTitleValidationRule implements ValidationRule {

    @Override
    public String getRuleId() {
        return "VN-009";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String jobTitle = event.getJobTitle();
        
        if (jobTitle != null && !jobTitle.isEmpty()) {
            if (jobTitle.length() < 3 || jobTitle.length() > 100) {
                result.addError(getRuleId(),
                        ValidationSeverity.WARNING,
                        "Cargo deve ter entre 3 e 100 caracteres",
                        "jobTitle",
                        jobTitle);
            }
        }
    }
}
