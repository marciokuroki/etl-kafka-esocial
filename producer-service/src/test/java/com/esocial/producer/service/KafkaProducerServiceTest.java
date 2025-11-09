package com.esocial.producer.service;

import com.esocial.producer.model.dto.EmployeeEventDTO;
import com.esocial.producer.model.dto.EventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do KafkaProducerService")
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<EmployeeEventDTO> eventCaptor;

    private KafkaProducerService producerService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        producerService = new KafkaProducerService(kafkaTemplate, meterRegistry);
        
        // Configurar tópicos via reflection
        ReflectionTestUtils.setField(producerService, "employeeCreateTopic", "employee-create");
        ReflectionTestUtils.setField(producerService, "employeeUpdateTopic", "employee-update");
        ReflectionTestUtils.setField(producerService, "employeeDeleteTopic", "employee-delete");
    }

    @Test
    @DisplayName("Deve publicar evento CREATE no tópico correto")
    void shouldPublishCreateEventToCorrectTopic() {
        // Given
        EmployeeEventDTO event = createTestEvent(EventType.CREATE);
        
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // When
        producerService.publishEmployeeEvent(event);

        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        assertThat(topicCaptor.getValue()).isEqualTo("employee-create");
        assertThat(keyCaptor.getValue()).isEqualTo(event.getEmployeeId());
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    @DisplayName("Deve publicar evento UPDATE no tópico correto")
    void shouldPublishUpdateEventToCorrectTopic() {
        // Given
        EmployeeEventDTO event = createTestEvent(EventType.UPDATE);
        
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // When
        producerService.publishEmployeeEvent(event);

        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), any(), any());
        assertThat(topicCaptor.getValue()).isEqualTo("employee-update");
    }

    @Test
    @DisplayName("Deve publicar evento DELETE no tópico correto")
    void shouldPublishDeleteEventToCorrectTopic() {
        // Given
        EmployeeEventDTO event = createTestEvent(EventType.DELETE);
        
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // When
        producerService.publishEmployeeEvent(event);

        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), any(), any());
        assertThat(topicCaptor.getValue()).isEqualTo("employee-delete");
    }

    @Test
    @DisplayName("Deve usar employeeId como chave da mensagem")
    void shouldUseEmployeeIdAsMessageKey() {
        // Given
        EmployeeEventDTO event = createTestEvent(EventType.CREATE);
        event.setEmployeeId("EMP123");
        
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // When
        producerService.publishEmployeeEvent(event);

        // Then
        verify(kafkaTemplate).send(any(), keyCaptor.capture(), any());
        assertThat(keyCaptor.getValue()).isEqualTo("EMP123");
    }

    @Test
    @DisplayName("Deve incrementar contador ao publicar com sucesso")
    void shouldIncrementCounterOnSuccessfulPublish() {
        // Given
        EmployeeEventDTO event = createTestEvent(EventType.CREATE);
        
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // When
        producerService.publishEmployeeEvent(event);
        future.join(); // Aguardar conclusão

        // Then
        Counter counter = meterRegistry.find("events.published").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Deve incrementar contador de falhas em caso de erro")
    void shouldIncrementFailureCounterOnError() {
        // Given
        EmployeeEventDTO event = createTestEvent(EventType.CREATE);
        
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            CompletableFuture.failedFuture(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // When
        producerService.publishEmployeeEvent(event);
        
        try {
            future.join();
        } catch (Exception e) {
            // Esperado
        }

        // Then
        Counter counter = meterRegistry.find("events.failed").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    // Helper method
    private EmployeeEventDTO createTestEvent(EventType eventType) {
        return EmployeeEventDTO.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventTimestamp(LocalDateTime.now())
                .employeeId("EMP001")
                .cpf("12345678901")
                .pis("10011223344")
                .fullName("João da Silva Santos")
                .birthDate(LocalDate.of(1985, 3, 15))
                .admissionDate(LocalDate.of(2020, 1, 10))
                .jobTitle("Analista de Sistemas")
                .department("TI")
                .salary(BigDecimal.valueOf(5500.00))
                .status("ACTIVE")
                .sourceSystem("HR_SYSTEM")
                .correlationId(UUID.randomUUID())
                .build();
    }
}
