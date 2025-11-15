package com.esocial.consumer.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.esocial.consumer.model.entity.DlqEvent;
import com.esocial.consumer.repository.DlqEventRepository;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"employee.events"})
@Testcontainers
@ActiveProfiles("test")
public class DlqRetryServiceIntegrationTest {

    @Autowired
    private DlqEventRepository dlqEventRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void testRetryProcessDLQ() {
        // Cria evento DLQ PENDENTE para retrial
        DlqEvent dlqEvent = new DlqEvent();
        dlqEvent.setEventPayload("{\"field\":\"value\"}");
        dlqEvent.setStatus("PENDING");
        dlqEvent.setRetryCount(0);
        dlqEvent.setEventId("evt-123");
        dlqEvent.setEventType("test");
        dlqEventRepository.save(dlqEvent);

        // O serviço de retry roda automaticamente conforme agendamento.
        // Para teste, pode chamar manualmente o método:
        // dlqRetryService.processDlqEvents();

        // Aguarde até o evento ser marcado como RETRIED
        Awaitility.await().untilAsserted(() -> {
            List<DlqEvent> events = dlqEventRepository.findByStatusAndRetryCountLessThan("PENDING", 5);
            assertThat(events).isEmpty();
            DlqEvent processedEvent = dlqEventRepository.findById(dlqEvent.getId()).orElseThrow();
            assertThat(processedEvent.getStatus()).isEqualTo("RETRIED");
            assertThat(processedEvent.getRetryCount()).isEqualTo(0);
        });
    }
}
