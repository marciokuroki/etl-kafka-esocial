package com.esocial.consumer.validation;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ValidationResultTest {

    @Test
    public void addErrorShouldMarkInvalid() {
        ValidationResult result = new ValidationResult();
        result.addError("Rule1", ValidationSeverity.ERROR, "Error message", "field", "value");
        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
    }
    
    @Test
    public void addWarningShouldNotMarkInvalid() {
        ValidationResult result = new ValidationResult();
        result.addError("Rule2", ValidationSeverity.WARNING, "Warning message", "field", "value");
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isTrue();
    }
}
