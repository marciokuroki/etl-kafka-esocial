package com.esocial.consumer.validation.rules;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.*;

import org.springframework.stereotype.Component;

@Component
public class CpfValidationRule implements ValidationRule {
    public String getRuleId() { return "VE-001"; }
    public ValidationSeverity getSeverity() { return ValidationSeverity.ERROR; }

    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String cpf = event.getCpf();
        if (cpf == null || !cpf.matches("\\d{11}") || !isValidCpf(cpf)) {
            result.addError(getRuleId(), getSeverity(), "CPF inválido", "cpf", cpf);
        }
    }

    // Lógica simplificada para CPF válido (troque por implementação robusta)
    private boolean isValidCpf(String cpf) {
        return !cpf.matches("(\\d)\\1{10}");
    }
}