package com.esocial.consumer.validation.rules.conformance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class UfValidationRule implements ValidationRule {

    private static final Set<String> VALID_UFS = new HashSet<>(
            Arrays.asList("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", 
                    "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", 
                    "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO")
    );

    @Override
    public String getRuleId() {
        return "VC-004";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String uf = event.getUf();
        
        if (uf != null && !uf.isEmpty()) {
            if (!VALID_UFS.contains(uf.toUpperCase())) {
                result.addError(getRuleId(), ValidationSeverity.WARNING, 
                    "UF deve ser uma sigla válida (ex: SP, RJ, MG)", 
                    "uf", uf);
            }
        }
    }
}

