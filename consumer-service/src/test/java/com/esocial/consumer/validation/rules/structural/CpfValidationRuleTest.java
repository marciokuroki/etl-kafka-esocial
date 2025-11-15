package com.esocial.consumer.validation.rules.structural;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class CpfValidationRuleTest {

    private CpfValidationRule rule;

    @BeforeEach
    public void setUp() {
        rule = new CpfValidationRule();
    }

    @Test
    public void shouldRejectNullCpf() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().cpf(null).build();
        ValidationResult result = new ValidationResult();
        rule.validate(dto, result);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> "CPF é obrigatório".equals(e.getMessage()));
    }

    @Test
    public void shouldRejectEmptyCpf() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().cpf("").build();
        ValidationResult result = new ValidationResult();
        rule.validate(dto, result);
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    public void shouldRejectCpfWithLessThan11Digits() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().cpf("1234567890").build();
        ValidationResult result = new ValidationResult();
        rule.validate(dto, result);
        assertThat(result.hasErrors()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "22222222222"})
    public void shouldRejectSequenceCpf(String cpf) {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().cpf(cpf).build();
        ValidationResult result = new ValidationResult();
        rule.validate(dto, result);
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    public void shouldAcceptValidCpf() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().cpf("12345678901").build();
        ValidationResult result = new ValidationResult();
        rule.validate(dto, result);
        assertThat(result.getErrors()).isEmpty();
    }
}
