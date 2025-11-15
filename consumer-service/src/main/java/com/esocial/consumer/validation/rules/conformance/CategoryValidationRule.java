package com.esocial.consumer.validation.rules.conformance;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationSeverity;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class CategoryValidationRule implements ValidationRule {

    private static final Set<String> VALID_CATEGORIES = new HashSet<>(
            Arrays.asList("101", "102", "103", "301", "305", "401")
    );

    @Override
    public String getRuleId() {
        return "VC-001";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String category = event.getCategory();
        
        if (category == null || category.isEmpty()) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, "Categoria é obrigatória", 
                "category", category);
            return;
        }

        if (!VALID_CATEGORIES.contains(category)) {
            result.addError(getRuleId(), ValidationSeverity.ERROR, 
                "Categoria deve ser uma das válidas conforme tabela eSocial", 
                "category", category);
        }
    }
}
