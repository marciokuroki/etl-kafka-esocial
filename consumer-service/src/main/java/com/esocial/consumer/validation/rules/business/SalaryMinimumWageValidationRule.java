package com.esocial.consumer.validation.rules.business;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class SalaryMinimumWageValidationRule implements ValidationRule {

    private static final BigDecimal MINIMUM_WAGE = new BigDecimal("1320.00");

    @Override
    public String getRuleId() {
        return "VN-007";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        BigDecimal salary = event.getSalary();
        
        if (salary != null && salary.compareTo(MINIMUM_WAGE) < 0) {
            result.addError(getRuleId(),
                    ValidationSeverity.WARNING,
                    "Salário inferior ao salário mínimo nacional (R$ 1.320,00)",
                    "salary",
                    salary.toString());
        }
    }
}
