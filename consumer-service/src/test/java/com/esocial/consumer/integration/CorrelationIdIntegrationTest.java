package com.esocial.consumer.integration;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.service.KafkaConsumerService;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.service.ValidationService;
import com.esocial.consumer.service.PersistenceService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CorrelationIdIntegrationTest {

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.3"));

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.2")
            .withDatabaseName("esocial")
            .withUsername("esocial_user")
            .withPassword("secret");

    @Autowired
    private KafkaConsumerService kafkaConsumerService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private PersistenceService persistenceService;

    @BeforeAll
    public void setup() {
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
        System.setProperty("spring.kafka.bootstrap-servers", kafka.getBootstrapServers());

        postgres.start();
        kafka.start();
    }

    @AfterAll
    public void tearDown() {
        kafka.stop();
        postgres.close();
        postgres.stop();
    }

    @Test
    public void testCorrelationIdPropagation() {
        UUID correlationId = UUID.randomUUID();

        EmployeeEventDTO event = EmployeeEventDTO.builder()
                .sourceId("TEST-123-" + UUID.randomUUID())
                .eventId(UUID.randomUUID().toString())
                .eventType("S-2300")
                .fullName("Test User Integration")
                .cpf("12345678901")
                .pis("10011223344")
                .correlationId(correlationId)
                .build();

        Headers headers = TestHelper.createHeadersWithCorrelationId(correlationId.toString());
        Acknowledgment acknowledgment = TestHelper.createAcknowledgmentStub();

        kafkaConsumerService.consumeEmployeeEvent(event, "employee-create", 1L, 0, headers, acknowledgment);

        ValidationResult result = validationService.validateAndPersistErrors(event, 0L, 0, "employee-create");
        Assertions.assertTrue(result.isValid(), "Evento deve ser válido");

        var employeeOpt = persistenceService.findEmployeeBySourceId(event.getSourceId());
        assertThat(employeeOpt).isPresent();
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
    }
}
