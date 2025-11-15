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
public class ContractTypeValidationRule implements ValidationRule {

    private static final Set<String> VALID_CONTRACT_TYPES = new HashSet<>(
            Arrays.asList("123", "124", "125", "126", "127", "128", "129")
    );

    @Override
    public String getRuleId() {
        return "VC-002";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String contractType = event.getContractType();
        
        if (contractType == null || contractType.isEmpty()) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "Tipo de contrato é obrigatório", 
                "contractType", contractType);
            return;
        }

        if (!VALID_CONTRACT_TYPES.contains(contractType)) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, 
                "Tipo de contrato deve estar conforme tabela 03 do eSocial", 
                "contractType", contractType);
        }
    }
}
