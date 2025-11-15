package com.esocial.consumer.validation.rules;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

public class BusinessValidationRuleTest {

    private BusinessValidationRule rule;

    @BeforeEach
    public void setUp() {
        rule = new BusinessValidationRule();
    }

    @Test
    public void shouldReportErrorForFutureBirthDate() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().birthDate(LocalDate.now().plusDays(1)).build();
        ValidationResult result = new ValidationResult();
        rule.validate(dto, result);
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("birthDate"));
    }

    @Test
    public void shouldReportWarningForSalaryBelowMinimum() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().salary(new BigDecimal("1000")).build();
        ValidationResult result = new ValidationResult();
        rule.validate(dto, result);
        assertThat(result.getWarnings()).anyMatch(w -> w.getField().equals("salary"));
    }
}
