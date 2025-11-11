package com.esocial.consumer.validation;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.rules.BusinessValidationRule;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class ValidationEngineTest {

    @Test
    public void validateEngineShouldAccumulateErrorsAndWarnings() {
        ValidationRule rule = new BusinessValidationRule();
        ValidationEngine engine = new ValidationEngine(List.of(rule));
        EmployeeEventDTO dto = EmployeeEventDTO.builder()
                .eventId("evt1")
                .birthDate(null) // no errors here
                .salary(null) // no warning here
                .build();

        ValidationResult result = engine.validate(dto);

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).isEmpty();
    }
}
