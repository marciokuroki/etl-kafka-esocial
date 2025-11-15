package com.esocial.consumer.validation.rules.business;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class SalaryMaximumValidationRule implements ValidationRule {

    private static final BigDecimal MAXIMUM_SALARY = new BigDecimal("1000000.00");

    @Override
    public String getRuleId() {
        return "VN-008";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        BigDecimal salary = event.getSalary();
        
        if (salary != null && salary.compareTo(MAXIMUM_SALARY) > 0) {
            result.addError(getRuleId(),
                    ValidationSeverity.WARNING,
                    "Salário excede limite razoável (R$ 1.000.000,00)",
                    "salary",
                    salary.toString());
        }
    }
}
