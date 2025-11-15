package com.esocial.consumer.validation.rules.structural;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationSeverity;
import org.springframework.stereotype.Component;

@Component
public class CpfValidationRule implements ValidationRule {

    @Override
    public String getRuleId() {
        return "VE-001";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String cpf = event.getCpf();
        
        if (cpf == null || cpf.isEmpty()) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "CPF é obrigatório", 
                "cpf", cpf);
            return;
        }
        
        if (!cpf.matches("\\d{11}")) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "CPF deve conter 11 dígitos numéricos", 
                "cpf", cpf);
            return;
        }
        
        if (!isValidCpf(cpf)) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "CPF com dígito verificador inválido", 
                "cpf", cpf);
        }
    }

    private boolean isValidCpf(String cpf) {
        return !cpf.matches("(\\d)\\1{10}");
    }
}
