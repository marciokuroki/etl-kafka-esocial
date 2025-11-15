package com.esocial.consumer.validation.rules.conformance;

import org.springframework.stereotype.Component;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationSeverity;

@Component
public class NationalityValidationRule implements ValidationRule {

    private static final String BRAZIL_CODE = "B";

    @Override
    public String getRuleId() {
        return "VC-005";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String nationality = event.getNationality();
        
        if (nationality != null && !nationality.isEmpty()) {
            // Brasileiro = "B", Estrangeiro = "E"
            if (!nationality.matches("[BE]")) {
                result.addError(getRuleId(), ValidationSeverity.ERROR, 
                    "Nacionalidade deve ser 'B' (Brasileiro) ou 'E' (Estrangeiro)", 
                    "nationality", nationality);
            }
        }
    }
}
