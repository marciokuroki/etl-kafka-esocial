package com.esocial.consumer.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    private boolean valid;
    
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();
    
    @Builder.Default
    private List<ValidationError> warnings = new ArrayList<>();
    
    public void addError(String rule, String message, String fieldName, String fieldValue) {
        errors.add(new ValidationError(rule, message, "ERROR", fieldName, fieldValue));
        valid = false;
    }
    
    public void addWarning(String rule, String message, String fieldName, String fieldValue) {
        warnings.add(new ValidationError(rule, message, "WARNING", fieldName, fieldValue));
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    @Data
    @AllArgsConstructor
    public static class ValidationError {
        private String rule;
        private String message;
        private String severity;
        private String fieldName;
        private String fieldValue;
    }
}
