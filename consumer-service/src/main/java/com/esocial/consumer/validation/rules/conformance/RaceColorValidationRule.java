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
public class RaceColorValidationRule implements ValidationRule {

    private static final Set<String> VALID_RACES = new HashSet<>(
            Arrays.asList("01", "02", "03", "04", "05", "06")
    );

    @Override
    public String getRuleId() {
        return "VC-007";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String race = event.getRace();
        
        if (race != null && !race.isEmpty()) {
            if (!VALID_RACES.contains(race)) {
                result.addError(getRuleId(), ValidationSeverity.WARNING, 
                    "Raça/cor deve estar conforme tabela eSocial", 
                    "race", race);
            }
        }
    }
}
