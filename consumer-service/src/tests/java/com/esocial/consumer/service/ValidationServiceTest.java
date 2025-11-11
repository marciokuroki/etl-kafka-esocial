package com.esocial.consumer.service;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationRule;
import com.esocial.consumer.validation.ValidationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class ValidationServiceTest {

    private ValidationService service;

    @BeforeEach
    public void setup() {
        ValidationRule mockRule = Mockito.mock(ValidationRule.class);
        ValidationEngine engine = new ValidationEngine(List.of(mockRule));
        ObjectMapper mapper = new ObjectMapper();
        service = new ValidationService(engine, null, mapper, new SimpleMeterRegistry());
    }

    @Test
    public void testValidateAndPersistErrors_shouldReturnValidationResult() {
        EmployeeEventDTO dto = EmployeeEventDTO.builder().build();
        ValidationResult result = service.validateAndPersistErrors(dto, 0L, 0, "topic");
        assertThat(result).isNotNull();
    }
}
