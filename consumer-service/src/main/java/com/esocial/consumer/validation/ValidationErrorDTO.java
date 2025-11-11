package com.esocial.consumer.validation;

import lombok.Getter;

@Getter
public class ValidationErrorDTO {
    private final String ruleId;
    private final ValidationSeverity severity;
    private final String message;
    private final String field;
    private final Object value;

    public ValidationErrorDTO(String ruleId, ValidationSeverity severity, String message, String field, Object value) {
        this.ruleId = ruleId;
        this.severity = severity;
        this.message = message;
        this.field = field;
        this.value = value;
    }
    // getters aqui (opcional: @Getter do Lombok)
}
