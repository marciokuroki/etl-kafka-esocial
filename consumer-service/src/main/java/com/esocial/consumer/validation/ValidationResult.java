package com.esocial.consumer.validation;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private final List<ValidationErrorDTO> errors = new ArrayList<>();
    private final List<ValidationErrorDTO> warnings = new ArrayList<>();
    private boolean valid = true;

    public void addError(String ruleId, ValidationSeverity severity, String message, String field, Object value) {
        ValidationErrorDTO ve = new ValidationErrorDTO(ruleId, severity, message, field, value);
        if (severity == ValidationSeverity.ERROR) {
            errors.add(ve);
            valid = false;
        } else if (severity == ValidationSeverity.WARNING) {
            warnings.add(ve);
        }
    }

    public List<ValidationErrorDTO> getErrors() {
        return errors;
    }

    public List<ValidationErrorDTO> getWarnings() {
        return warnings;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean isValid() {
        return valid;
    }
}
