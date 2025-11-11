package com.esocial.consumer.validation.rules;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StructuralValidationRuleTest {

    private StructuralValidationRule rule;

    @BeforeEach
    public void setup() {
        rule = new StructuralValidationRule();
    }

    @Test
    public void shouldDetectInvalidCpf() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().cpf("123456789").build();
        ValidationResult result = new ValidationResult();

        rule.validate(dto, result);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .anyMatch(e -> e.getField().equals("cpf") && e.getMessage().contains("11 dígitos"));
    }

    @Test
    public void shouldPassValidCpf() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().cpf("12345678901").build();
        ValidationResult result = new ValidationResult();

        rule.validate(dto, result);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    public void shouldDetectInvalidPis() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().pis("12345").build();
        ValidationResult result = new ValidationResult();

        rule.validate(dto, result);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .anyMatch(e -> e.getField().equals("pis") && e.getMessage().contains("11 dígitos"));
    }

    @Test
    public void shouldPassValidPis() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().pis("12345678901").build();
        ValidationResult result = new ValidationResult();

        rule.validate(dto, result);

        assertThat(result.hasErrors()).isFalse();
    }
}
