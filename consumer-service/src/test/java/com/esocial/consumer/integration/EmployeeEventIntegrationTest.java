package com.esocial.consumer.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.esocial.consumer.repository.EmployeeEventRepository;
import com.esocial.consumer.repository.EmployeeRepository;

@SpringBootTest(classes = com.esocial.consumer.ConsumerApplication.class)
@EmbeddedKafka(partitions = 1, topics = {"employee.events"})
@Testcontainers
public class EmployeeEventIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeEventRepository eventRepository;

    @Test
    public void testFullFlowCreateEmployeeEvent() throws Exception {
        String json = "{"
            + "\"source_id\": \"HR-SYSTEM-001-12345\","
            + "\"event_id\": \"evt-20251110-98765\","
            + "\"event_type\": \"S-2300\","
            + "\"correlation_id\": \"550e8400-e29b-41d4-a716-446655440000\","
            + "\"cpf\": \"12345678901\","
            + "\"pis\": \"17033259504\","
            + "\"ctps\": \"1234567/SP\","
            + "\"matricula\": \"EMP-001\","
            + "\"full_name\": \"João da Silva\","
            + "\"birth_date\": \"1990-05-15\","
            + "\"admission_date\": \"2020-01-10\","
            + "\"email\": \"joao@example.com\","
            + "\"salary\": 5000.00,"
            + "\"status\": \"ACTIVE\","
            + "\"kafka_topic\": \"employee.events\","
            + "\"kafka_partition\": 0,"
            + "\"kafka_offset\": 12345"
            + "}";

        // Envia a mensagem (fire and forget)
        kafkaTemplate.send("employee.events", json);

        // Aguarda o processamento e assertivas no banco
        Awaitility.await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                assertThat(employeeRepository.findBySourceId("HR-SYSTEM-001-12345")).isPresent();
                assertThat(eventRepository.findBySourceId("HR-SYSTEM-001-12345")).isPresent();
            });
    }
}
