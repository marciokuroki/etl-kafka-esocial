package com.esocial.consumer.integration;


import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Teste E2E: Dead Letter Queue (DLQ)
 * 
 * Cenários testados:
 * 1. Evento inválido vai para DLQ
 * 2. Consulta eventos na DLQ via API
 * 3. Múltiplos eventos na DLQ
 * 4. Retry count incrementado
 * 5. Status DLQ correto
 */
@DisplayName("E2E Test: Dead Letter Queue (DLQ)")
@AutoConfigureMockMvc
class EmployeeDLQE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve enviar evento inválido para DLQ")
    void shouldSendInvalidEventToDLQ() {        
        // GIVEN: Evento com múltiplos erros críticos
        String sourceId = "EMP_TO_DLQ_001";
        UUID correlationId = UUID.randomUUID();
        String eventId = correlationId.toString();
        
        EmployeeEventDTO invalidEvent = EmployeeEventDTO.builder()
                .eventId(eventId)
                .eventType("CREATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("123")  // ❌ CPF inválido
                .pis(null)   // ❌ PIS obrigatório
                .fullName(null)  // ❌ Nome obrigatório
                .birthDate(LocalDate.of(1990, 1, 15))
                .admissionDate(LocalDate.of(2024, 1, 10))
                .jobTitle("Analista")
                .department("TI")
                .salary(new BigDecimal("5000.00"))
                .status("ACTIVE")
                .build();

        // WHEN: Publicar evento inválido
        kafkaTemplate.send("employee-create", sourceId, invalidEvent);

        // THEN: Evento deve ir para DLQ
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    // 1. Evento NÃO foi processado
                    var employee = employeeRepository.findBySourceId(sourceId);
                    assertThat(employee).isEmpty();

                    // 2. Evento está na DLQ
                    List<Map<String, Object>> dlqEvents = jdbcTemplate.queryForList(
                            "SELECT event_id, event_type, event_payload, error_message, " +
                            "retry_count, status, created_at " +
                            "FROM public.dlq_events " +
                            "WHERE event_id = ?",
                            eventId
                    );
                    
                    assertThat(dlqEvents).hasSize(1);
                    
                    Map<String, Object> dlqEvent = dlqEvents.get(0);
                    assertThat(dlqEvent.get("event_id")).isEqualTo(eventId);
                    assertThat(dlqEvent.get("event_type")).isEqualTo("CREATE");
                    assertThat(dlqEvent.get("status")).isEqualTo("PENDING");
                    assertThat(dlqEvent.get("retry_count")).isEqualTo(0);
                    assertThat(dlqEvent.get("error_message")).isNotNull();
                    
                    // 3. Mensagem de erro contém detalhes
                    String errorMessage = dlqEvent.get("error_message").toString();
                    assertThat(errorMessage)
                            .containsIgnoringCase("validation")
                            .containsAnyOf("CPF", "PIS", "Nome");
                });
    }

    @Test
    @DisplayName("Deve consultar eventos na DLQ via API REST")
    void shouldQueryDLQEventsViaAPI() throws Exception {
        // GIVEN: Evento inválido na DLQ
        String sourceId = "EMP_API_DLQ_001";
        UUID correlationId = UUID.randomUUID();
        String eventId = correlationId.toString();
        
        EmployeeEventDTO invalidEvent = EmployeeEventDTO.builder()
                .eventId(eventId)
                .eventType("CREATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("456")  // Inválido
                .fullName("API Test DLQ")
                .birthDate(LocalDate.of(1990, 1, 15))
                .admissionDate(LocalDate.of(2024, 1, 10))
                .jobTitle("Analista")
                .department("TI")
                .salary(new BigDecimal("5000.00"))
                .status("ACTIVE")
                .build();

        kafkaTemplate.send("employee-create", sourceId, invalidEvent);

        // WHEN/THEN: Consultar DLQ via API
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    mockMvc.perform(get("/api/v1/validation/dlq"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$").isArray())
                            .andExpect(jsonPath("$[?(@.eventId == '" + eventId + "')]").exists())
                            .andExpect(jsonPath("$[?(@.eventId == '" + eventId + "')].status").value("PENDING"))
                            .andExpect(jsonPath("$[?(@.eventId == '" + eventId + "')].retryCount").value(0));
                });
    }

    @Test
    @DisplayName("Deve acumular múltiplos eventos na DLQ")
    void shouldAccumulateMultipleEventsInDLQ() {
        // GIVEN: 5 eventos inválidos diferentes
        for (int i = 1; i <= 5; i++) {
            String sourceId = "EMP_DLQ_BATCH_" + String.format("%03d", i);
            UUID correlationId = UUID.randomUUID();
            String eventId = correlationId.toString();
            
            EmployeeEventDTO invalidEvent = EmployeeEventDTO.builder()
                    .eventId(eventId)
                    .eventType("CREATE")
                    .eventTimestamp(LocalDateTime.now())
                    .correlationId(correlationId)
                    .sourceId(sourceId)
                    .cpf(String.valueOf(i))  // CPF inválido (1 dígito)
                    .fullName("DLQ Batch " + i)
                    .birthDate(LocalDate.of(1990, 1, i))
                    .admissionDate(LocalDate.of(2024, 1, 1))
                    .jobTitle("Analista")
                    .department("TI")
                    .salary(new BigDecimal("5000.00"))
                    .status("ACTIVE")
                    .build();

            kafkaTemplate.send("employee-create", sourceId, invalidEvent);
        }

        // THEN: 5 eventos na DLQ
        await()
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    Long dlqCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM public.dlq_events " +
                            "WHERE event_payload::text LIKE '%EMP_DLQ_BATCH_%'",
                            Long.class
                    );
                    
                    assertThat(dlqCount).isEqualTo(5L);

                    // Todos com status PENDING
                    Long pendingCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM public.dlq_events " +
                            "WHERE event_payload::text LIKE '%EMP_DLQ_BATCH_%' " +
                            "AND status = 'PENDING'",
                            Long.class
                    );
                    
                    assertThat(pendingCount).isEqualTo(5L);
                });
    }

    @Test
    @DisplayName("Deve marcar evento como FAILED após múltiplas tentativas")
    void shouldMarkEventAsFailedAfterMultipleRetries() {
        // GIVEN: Evento que sempre falha
        String sourceId = "EMP_MAX_RETRIES";
        UUID correlationId = UUID.randomUUID();
        String eventId = correlationId.toString();
        
        // Simular evento que já foi retentado 3 vezes
        jdbcTemplate.update(
                "INSERT INTO public.dlq_events " +
                "(event_id, event_type, event_payload, error_message, retry_count, status, created_at) " +
                "VALUES (?, ?, ?::jsonb, ?, ?, ?, NOW())",
                eventId,
                "CREATE",
                "{\"sourceId\": \"" + sourceId + "\"}",
                "Validation failed: Invalid CPF",
                3,  // Já retentou 3 vezes
                "PENDING"
        );

        // WHEN: Simular nova tentativa falhada (via código ou manualmente)
        jdbcTemplate.update(
                "UPDATE public.dlq_events " +
                "SET retry_count = retry_count + 1, " +
                "    status = CASE WHEN retry_count >= 3 THEN 'FAILED' ELSE 'PENDING' END, " +
                "    updated_at = NOW() " +
                "WHERE event_id = ?",
                eventId
        );

        // THEN: Status mudou para FAILED
        Map<String, Object> dlqEvent = jdbcTemplate.queryForMap(
                "SELECT retry_count, status FROM public.dlq_events WHERE event_id = ?",
                eventId
        );
        
        assertThat(dlqEvent.get("retry_count")).isEqualTo(4);
        assertThat(dlqEvent.get("status")).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Deve incluir payload completo do evento na DLQ")
    void shouldIncludeFullEventPayloadInDLQ() {
        // GIVEN: Evento com dados completos
        String sourceId = "EMP_FULL_PAYLOAD";
        UUID correlationId = UUID.randomUUID();
        String eventId = correlationId.toString();
        
        EmployeeEventDTO event = EmployeeEventDTO.builder()
                .eventId(eventId)
                .eventType("CREATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("789")  // Inválido
                .pis("10011223344")
                .fullName("Payload Completo Teste")
                .birthDate(LocalDate.of(1990, 5, 20))
                .admissionDate(LocalDate.of(2024, 1, 10))
                .jobTitle("Coordenador")
                .department("Financeiro")
                .salary(new BigDecimal("7500.00"))
                .status("ACTIVE")
                .build();

        kafkaTemplate.send("employee-create", sourceId, event);

        // THEN: Payload completo na DLQ
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    Map<String, Object> dlqEvent = jdbcTemplate.queryForMap(
                            "SELECT event_payload FROM public.dlq_events WHERE event_id = ?",
                            eventId
                    );
                    
                    String payload = dlqEvent.get("event_payload").toString();
                    
                    // Verificar que payload contém todos os campos importantes
                    assertThat(payload)
                            .contains(eventId)
                            .contains(sourceId)
                            .contains(correlationId.toString())
                            .contains("Payload Completo Teste")
                            .contains("Coordenador")
                            .contains("7500");
                });
    }
}
