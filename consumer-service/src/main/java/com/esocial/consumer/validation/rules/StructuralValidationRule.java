package com.esocial.consumer.validation.rules;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StructuralValidationRule implements ValidationRule {
    
    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        log.debug("Executando validação estrutural para evento: {}", event.getEventId());
        
        // Validar campos obrigatórios
        if (event.getEmployeeId() == null || event.getEmployeeId().isBlank()) {
            result.addError(getRuleId(), 
                ValidationSeverity.ERROR,
                "ID do colaborador é obrigatório", 
                "employeeId", 
                null);
        }
        
        if (event.getCpf() == null || event.getCpf().isBlank()) {
            result.addError(getRuleId(), 
                ValidationSeverity.ERROR,
                "CPF é obrigatório", 
                "cpf", 
                null);
        }
        
        if (event.getFullName() == null || event.getFullName().isBlank()) {
            result.addError(getRuleId(), 
                ValidationSeverity.ERROR, "Nome completo é obrigatório", "fullName", null);
        }
        
        if (event.getAdmissionDate() == null) {
            result.addError(getRuleId(), 
                ValidationSeverity.ERROR, "Data de admissão é obrigatória", "admissionDate", null);
        }
        
        // Validar formato CPF (11 dígitos)
        if (event.getCpf() != null && !event.getCpf().matches("\\d{11}")) {
            result.addError(getRuleId(), 
                ValidationSeverity.ERROR, "CPF deve conter exatamente 11 dígitos numéricos", 
                    "cpf", event.getCpf());
        }
        
        // Validar formato PIS (11 dígitos, se informado)
        if (event.getPis() != null && !event.getPis().isBlank() && !event.getPis().matches("\\d{11}")) {
            result.addError(getRuleId(), 
                ValidationSeverity.ERROR, "PIS deve conter exatamente 11 dígitos numéricos", 
                    "pis", event.getPis());
        }
        
        // Validar salário (deve ser positivo)
        if (event.getSalary() != null && event.getSalary().doubleValue() <= 0) {
            result.addError(getRuleId(), 
                ValidationSeverity.ERROR, "Salário deve ser maior que zero", 
                    "salary", event.getSalary().toString());
        }
        
        log.debug("Validação estrutural concluída. Válido: {}, Erros: {}", 
                !result.hasErrors(), result.getErrors().size());
    }
    
    @Override
    public String getRuleId() {
        return "STRUCTURAL_VALIDATION";
    }
}
