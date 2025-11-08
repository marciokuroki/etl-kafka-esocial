package com.esocial.producer.service;

import com.esocial.producer.model.dto.EmployeeEventDTO;
import com.esocial.producer.model.dto.EventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;
    private final Counter eventsPublishedCounter;
    private final Counter eventsFailedCounter;

    @Value("${app.kafka.topics.employee-create}")
    private String employeeCreateTopic;

    @Value("${app.kafka.topics.employee-update}")
    private String employeeUpdateTopic;

    @Value("${app.kafka.topics.employee-delete}")
    private String employeeDeleteTopic;

    public KafkaProducerService(
            KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventsPublishedCounter = Counter.builder("events.published")
                .description("Total de eventos publicados no Kafka")
                .tag("service", "producer")
                .register(meterRegistry);
        this.eventsFailedCounter = Counter.builder("events.failed")
                .description("Total de eventos que falharam ao publicar")
                .tag("service", "producer")
                .register(meterRegistry);
    }

    /**
     * Publica evento de colaborador no Kafka
     */
    public void publishEmployeeEvent(EmployeeEventDTO event) {
        String topic = determineTopicByEventType(event.getEventType());
        String key = event.getEmployeeId(); // Usa CPF ou ID como chave para garantir ordem

        log.debug("Publicando evento no tópico {}: eventId={}, employeeId={}, type={}",
                topic, event.getEventId(), event.getEmployeeId(), event.getEventType());

        CompletableFuture<SendResult<String, EmployeeEventDTO>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                eventsPublishedCounter.increment();
                log.info("Evento publicado com sucesso: topic={}, partition={}, offset={}, eventId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getEventId());
            } else {
                eventsFailedCounter.increment();
                log.error("Erro ao publicar evento: eventId={}, error={}",
                        event.getEventId(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * Determina o tópico baseado no tipo de evento
     */
    private String determineTopicByEventType(EventType eventType) {
        return switch (eventType) {
            case CREATE -> employeeCreateTopic;
            case UPDATE -> employeeUpdateTopic;
            case DELETE -> employeeDeleteTopic;
        };
    }
}
