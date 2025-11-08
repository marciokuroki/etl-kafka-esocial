package com.esocial.consumer.validation.rules;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class BusinessValidationRule implements ValidationRule {
    
    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        log.debug("Executando validação de regras de negócio para evento: {}", event.getEventId());
        
        // Validar data de nascimento (não pode ser futura, idade mínima 16 anos)
        if (event.getBirthDate() != null) {
            if (event.getBirthDate().isAfter(LocalDate.now())) {
                result.addError(getRuleName(), "Data de nascimento não pode ser futura", 
                        "birthDate", event.getBirthDate().toString());
            }
            
            int age = LocalDate.now().getYear() - event.getBirthDate().getYear();
            if (age < 16) {
                result.addError(getRuleName(), "Colaborador deve ter pelo menos 16 anos", 
                        "birthDate", event.getBirthDate().toString());
            }
            
            if (age > 120) {
                result.addWarning(getRuleName(), "Idade do colaborador é incomum (>120 anos)", 
                        "birthDate", event.getBirthDate().toString());
            }
        }
        
        // Validar data de admissão (não pode ser futura)
        if (event.getAdmissionDate() != null && event.getAdmissionDate().isAfter(LocalDate.now())) {
            result.addError(getRuleName(), "Data de admissão não pode ser futura", 
                    "admissionDate", event.getAdmissionDate().toString());
        }
        
        // Validar data de demissão (deve ser posterior à admissão)
        if (event.getTerminationDate() != null && event.getAdmissionDate() != null) {
            if (event.getTerminationDate().isBefore(event.getAdmissionDate())) {
                result.addError(getRuleName(), 
                        "Data de demissão não pode ser anterior à data de admissão", 
                        "terminationDate", event.getTerminationDate().toString());
            }
        }
        
        // Validar admissão após nascimento (idade mínima na admissão)
        if (event.getBirthDate() != null && event.getAdmissionDate() != null) {
            if (event.getAdmissionDate().isBefore(event.getBirthDate().plusYears(16))) {
                result.addError(getRuleName(), 
                        "Colaborador deve ter pelo menos 16 anos na data de admissão", 
                        "admissionDate", event.getAdmissionDate().toString());
            }
        }
        
        // Validar salário mínimo (exemplo: R$ 1320,00 em 2024)
        if (event.getSalary() != null && event.getSalary().doubleValue() < 1320.00) {
            result.addWarning(getRuleName(), 
                    "Salário está abaixo do salário mínimo nacional", 
                    "salary", event.getSalary().toString());
        }
        
        // Validar status coerente com data de demissão
        if ("ACTIVE".equals(event.getStatus()) && event.getTerminationDate() != null) {
            result.addError(getRuleName(), 
                    "Colaborador não pode estar ATIVO com data de demissão preenchida", 
                    "status", event.getStatus());
        }
        
        log.debug("Validação de negócio concluída. Erros: {}, Warnings: {}", 
                result.getErrors().size(), result.getWarnings().size());
    }
    
    @Override
    public String getRuleName() {
        return "BUSINESS_VALIDATION";
    }
}
