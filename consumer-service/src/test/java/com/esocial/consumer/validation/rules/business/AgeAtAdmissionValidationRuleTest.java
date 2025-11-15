package com.esocial.consumer.validation.rules.business;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

public class AgeAtAdmissionValidationRuleTest {

    private AgeAtAdmissionValidationRule rule;

    @BeforeEach
    public void setUp() {
        rule = new AgeAtAdmissionValidationRule();
    }

    @Test
    public void shouldRejectAdmissionBefore16Years() {
        LocalDate birthDate = LocalDate.now().minusYears(15);
        LocalDate admissionDate = LocalDate.now();
        
        EmployeeEventDTO dto = EmployeeEventDTO.builder()
                .birthDate(birthDate)
                .admissionDate(admissionDate)
                .build();
        
        ValidationResult result = new ValidationResult();
        rule.validate(dto, result);
        
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> e.getRuleId().equals("VN-001"));
    }

    @Test
    public void shouldAcceptAdmissionAt16Years() {
        LocalDate birthDate = LocalDate.now().minusYears(16);
        LocalDate admissionDate = LocalDate.now();
        
        EmployeeEventDTO dto = EmployeeEventDTO.builder()
                .birthDate(birthDate)
                .admissionDate(admissionDate)
                .build();
        
        ValidationResult result = new ValidationResult();
        rule.validate(dto, result);
        
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    public void shouldIgnoreWhenBirthDateIsNull() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder()
                .birthDate(null)
                .admissionDate(LocalDate.now())
                .build();
        
        ValidationResult result = new ValidationResult();
        rule.validate(dto, result);
        
        assertThat(result.hasErrors()).isFalse();
    }
}
