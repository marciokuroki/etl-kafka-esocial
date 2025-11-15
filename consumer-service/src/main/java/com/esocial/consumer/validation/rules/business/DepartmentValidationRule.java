package com.esocial.consumer.validation.rules.business;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class DepartmentValidationRule implements ValidationRule {

    @Override
    public String getRuleId() {
        return "VN-010";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String department = event.getDepartment();
        
        if (department != null && !department.isEmpty()) {
            if (department.length() < 2 || department.length() > 100) {
                result.addError(getRuleId(),
                        ValidationSeverity.WARNING,
                        "Departamento deve ter entre 2 e 100 caracteres",
                        "department",
                        department);
            }
        }
    }
}
