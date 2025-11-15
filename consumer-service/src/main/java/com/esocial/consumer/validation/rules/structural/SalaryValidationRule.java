package com.esocial.consumer.validation.rules.structural;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class SalaryValidationRule implements ValidationRule {
    private static final BigDecimal MINIMUM_SALARY = new BigDecimal("1320.00");
    
    @Override
    public String getRuleId() { return "VE-006"; }
    
    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        BigDecimal salary = event.getSalary();
        if (salary == null || salary.compareTo(BigDecimal.ZERO) <= 0) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "Salário deve ser maior que zero", 
                "salary", salary);
            return;
        }
        if (salary.compareTo(MINIMUM_SALARY) < 0) {
            result.addError(getRuleId(), ValidationSeverity.WARNING, "Salário inferior ao mínimo legal (R$ 1.320,00)", 
                "salary", salary);
        }
    }
}
