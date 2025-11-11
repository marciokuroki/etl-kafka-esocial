package com.esocial.consumer.validation.rules;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.*;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class SalarioMinimoValidationRule implements ValidationRule {
    public String getRuleId() { return "VN-007"; }
    public ValidationSeverity getSeverity() { return ValidationSeverity.WARNING; }

    public void validate(EmployeeEventDTO event, ValidationResult result) {
        BigDecimal salario = event.getSalary();
        if (salario == null || salario.compareTo(new BigDecimal("1320.00")) < 0) {
            result.addError(getRuleId(), getSeverity(), "Salário inferior ao mínimo legal (R$ 1.320,00)", "salary", salario);
        }
    }
}
