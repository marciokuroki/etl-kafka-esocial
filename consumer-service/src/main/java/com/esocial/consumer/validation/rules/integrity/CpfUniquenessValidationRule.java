package com.esocial.consumer.validation.rules.integrity;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.repository.EmployeeRepository;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class CpfUniquenessValidationRule implements ValidationRule {
    private final EmployeeRepository employeeRepository;

    public CpfUniquenessValidationRule(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public String getRuleId() {
        return "VI-001";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String cpf = event.getCpf();
        String sourceId = event.getSourceId(); 
        
        if (cpf != null && !cpf.isEmpty()) {
            long count = employeeRepository.countByCpfAndSourceIdNot(cpf, sourceId);
            if (count > 0) {
                result.addError(getRuleId(), ValidationSeverity.ERROR, 
                    "CPF já cadastrado na base", 
                    "cpf", cpf);
            }
        }
    }
}
