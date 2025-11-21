package com.esocial.consumer.integration;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.service.KafkaConsumerService;
import com.esocial.consumer.service.PersistenceService;
import com.esocial.consumer.service.ValidationService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@EmbeddedKafka(partitions = 1, topics = { "employee-create", "employee-update", "employee-delete" })
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConsumerServiceIntegrationTest {

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
    public void testFullEmployeeFlow() throws InterruptedException {
        // Criar evento de teste único
        EmployeeEventDTO event = EmployeeEventDTO.builder()
                .sourceId("TEST-123-" + UUID.randomUUID())
                .eventId(UUID.randomUUID().toString())
                .eventType("S-2300")
                .fullName("Test User Integration")
                .cpf("12345678901")
                .pis("10011223344")
                .build();

        // Simular consumo do evento
        kafkaConsumerService.consumeEmployeeEvent(event, "employee-create", 1L, 0, () -> {});

        // Validar o evento via ValidationService
        ValidationResult result = validationService.validateAndPersistErrors(event, 0L, 0, "employee-create");
        Assertions.assertTrue(result.isValid(), "Evento deve ser válido");

        // Validar persistência do empregado via PersistenceService
        var employeeOpt = persistenceService.findEmployeeBySourceId(event.getSourceId());
        assertThat(employeeOpt).isPresent();
    }
}