package com.esocial.producer.service;

import com.esocial.producer.model.dto.EmployeeEventDTO;
import com.esocial.producer.model.dto.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KafkaProducerServiceTest {

    private KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;
    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    private KafkaProducerService producerService;

    @BeforeEach
    void setup() {
        // Mocks
        kafkaTemplate = mock(KafkaTemplate.class);
        meterRegistry = new SimpleMeterRegistry(); // In-memory registry para testes
        objectMapper = new ObjectMapper();
        
        // Instanciar serviço
        producerService = new KafkaProducerService(kafkaTemplate, meterRegistry, objectMapper);
        
        // ✅ Injetar valores dos @Value usando ReflectionTestUtils
        ReflectionTestUtils.setField(producerService, "employeeCreateTopic", "employee-create");
        ReflectionTestUtils.setField(producerService, "employeeUpdateTopic", "employee-update");
        ReflectionTestUtils.setField(producerService, "employeeDeleteTopic", "employee-delete");
    }

    @Test
    @DisplayName("Deve publicar evento CREATE com sucesso e incrementar counter")
    void shouldPublishCreateEventAndIncrementCounter() {
        // Arrange
        EmployeeEventDTO event = createSampleEvent(EventType.CREATE, "EMP123");
        
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            mockSuccessfulKafkaSend("employee-create", 0, 100L);
        
        when(kafkaTemplate.send(eq("employee-create"), eq("EMP123"), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // Act
        producerService.publishEmployeeEvent(event);

        // Assert
        // ✓ Verifica counter de publicação
        Counter publishedCounter = meterRegistry.find("events.published").counter();
        assertThat(publishedCounter).isNotNull();
        assertThat(publishedCounter.count()).isEqualTo(1.0);
        
        // ✓ Verifica counter detalhado com tags
        Counter detailedCounter = meterRegistry.find("events.published.total")
                .tag("event_type", "CREATE")
                .counter();
        assertThat(detailedCounter).isNotNull();
        assertThat(detailedCounter.count()).isEqualTo(1.0);

        // ✓ Verifica registro de tamanho de payload
        DistributionSummary payloadSummary = meterRegistry.find("events.payload.size").summary();
        assertThat(payloadSummary).isNotNull();
        assertThat(payloadSummary.count()).isEqualTo(1);
        assertThat(payloadSummary.totalAmount()).isGreaterThan(0);

        // ✓ Verifica timer de latência
        Timer timer = meterRegistry.find("kafka.publish.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);

        // ✓ Verifica chamada ao Kafka
        verify(kafkaTemplate, times(1)).send("employee-create", "EMP123", event);
    }

    @Test
    @DisplayName("Deve incrementar counter de falha quando ocorrer erro no Kafka")
    void shouldIncrementFailedCounterOnKafkaError() {
        // Arrange
        EmployeeEventDTO event = createSampleEvent(EventType.UPDATE, "EMP124");
        
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            mockFailedKafkaSend(new RuntimeException("Kafka broker not available"));
        
        when(kafkaTemplate.send(eq("employee-update"), eq("EMP124"), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // Act
        producerService.publishEmployeeEvent(event);

        // Assert
        // ✓ Verifica counter de falha
        Counter failedCounter = meterRegistry.find("events.failed").counter();
        assertThat(failedCounter).isNotNull();
        assertThat(failedCounter.count()).isEqualTo(1.0);
        
        // ✓ Verifica counter detalhado com tags
        Counter detailedFailedCounter = meterRegistry.find("events.failed.total")
                .tag("event_type", "UPDATE")
                .tag("error_type", "RuntimeException")
                .counter();
        assertThat(detailedFailedCounter).isNotNull();
        assertThat(detailedFailedCounter.count()).isEqualTo(1.0);

        // ✓ Verifica que NÃO incrementou counter de sucesso
        Counter publishedCounter = meterRegistry.find("events.published").counter();
        assertThat(publishedCounter).isNull(); // Não foi criado porque não houve sucesso
    }

    @Test
    @DisplayName("Deve enviar eventos CREATE para o tópico correto")
    void shouldSendCreateEventToCorrectTopic() {
        // Arrange
        EmployeeEventDTO event = createSampleEvent(EventType.CREATE, "EMP125");
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            mockSuccessfulKafkaSend("employee-create", 0, 200L);
        
        when(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // Act
        producerService.publishEmployeeEvent(event);

        // Assert
        verify(kafkaTemplate).send(eq("employee-create"), eq("EMP125"), eq(event));
    }

    @Test
    @DisplayName("Deve enviar eventos UPDATE para o tópico correto")
    void shouldSendUpdateEventToCorrectTopic() {
        // Arrange
        EmployeeEventDTO event = createSampleEvent(EventType.UPDATE, "EMP126");
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            mockSuccessfulKafkaSend("employee-update", 1, 300L);
        
        when(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // Act
        producerService.publishEmployeeEvent(event);

        // Assert
        verify(kafkaTemplate).send(eq("employee-update"), eq("EMP126"), eq(event));
    }

    @Test
    @DisplayName("Deve enviar eventos DELETE para o tópico correto")
    void shouldSendDeleteEventToCorrectTopic() {
        // Arrange
        EmployeeEventDTO event = createSampleEvent(EventType.DELETE, "EMP127");
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            mockSuccessfulKafkaSend("employee-delete", 2, 400L);
        
        when(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // Act
        producerService.publishEmployeeEvent(event);

        // Assert
        verify(kafkaTemplate).send(eq("employee-delete"), eq("EMP127"), eq(event));
    }

    @Test
    @DisplayName("Deve registrar tamanho estimado do payload corretamente")
    void shouldEstimatePayloadSizeCorrectly() {
        // Arrange
        EmployeeEventDTO event = EmployeeEventDTO.builder()
                .eventId("evt-001")
                .eventType(EventType.CREATE)
                .employeeId("EMP128")
                .cpf("12345678901")
                .pis("10011223344")
                .fullName("João da Silva Santos") // 22 chars = 44 bytes
                .jobTitle("Analista de Sistemas") // 21 chars = 42 bytes
                .department("Tecnologia da Informação") // 27 chars = 54 bytes
                .status("ACTIVE") // 6 chars = 12 bytes
                .sourceSystem("HR_SYSTEM") // 9 chars = 18 bytes
                .salary(BigDecimal.valueOf(5500.00))
                .admissionDate(LocalDate.of(2024, 1, 10))
                .eventTimestamp(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = 
            mockSuccessfulKafkaSend("employee-create", 0, 500L);
        when(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEventDTO.class)))
            .thenReturn(future);

        // Act
        producerService.publishEmployeeEvent(event);

        // Assert
        DistributionSummary payloadSummary = meterRegistry.find("events.payload.size").summary();
        assertThat(payloadSummary).isNotNull();
        assertThat(payloadSummary.totalAmount()).isGreaterThan(250); // Base size
        assertThat(payloadSummary.totalAmount()).isLessThan(1000); // Limite razoável
    }

    // ========== Métodos Auxiliares ==========

    /**
     * Cria um evento de exemplo para testes
     */
    private EmployeeEventDTO createSampleEvent(EventType eventType, String employeeId) {
        return EmployeeEventDTO.builder()
                .eventId("evt-" + System.currentTimeMillis())
                .eventType(eventType)
                .eventTimestamp(LocalDateTime.now())
                .employeeId(employeeId)
                .cpf("12345678901")
                .pis("10011223344")
                .fullName("Teste Silva")
                .jobTitle("Analista")
                .department("TI")
                .salary(BigDecimal.valueOf(5000.00))
                .status("ACTIVE")
                .admissionDate(LocalDate.of(2024, 1, 10))
                .sourceSystem("HR_SYSTEM")
                .build();
    }

    /**
     * Cria um CompletableFuture mockado para envio bem-sucedido
    */
    private CompletableFuture<SendResult<String, EmployeeEventDTO>> mockSuccessfulKafkaSend(
        String topic, int partition, long offset) {
    
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = new CompletableFuture<>();
        
        SendResult<String, EmployeeEventDTO> sendResult = mock(SendResult.class);
            
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(topic, partition),  // TopicPartition
                offset,                                 // offset
                0,                                      // batchIndex (int, não Long)
                System.currentTimeMillis(),             // timestamp
                0,                                      // serializedKeySize
                0                                       // serializedValueSize
        );
        
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        future.complete(sendResult);
        
        return future;
    }


    /**
     * Cria um CompletableFuture mockado para envio com falha
     */
    private CompletableFuture<SendResult<String, EmployeeEventDTO>> mockFailedKafkaSend(
            Exception exception) {
        
        CompletableFuture<SendResult<String, EmployeeEventDTO>> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        return future;
    }
}
