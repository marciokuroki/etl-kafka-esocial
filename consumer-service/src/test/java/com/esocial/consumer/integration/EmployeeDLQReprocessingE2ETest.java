package com.esocial.consumer.integration;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste E2E: Reprocessamento de eventos da DLQ
 * 
 * Cenários testados:
 * 1. Reprocessar evento corrigido com sucesso
 * 2. Reprocessar evento ainda inválido
 * 3. Incrementar retry_count
 * 4. Marcar como REPROCESSED após sucesso
 * 5. Consultar dashboard de DLQ
 */
@DisplayName("E2E Test: Reprocessamento DLQ")
@AutoConfigureMockMvc
class EmployeeDLQReprocessingE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve reprocessar evento da DLQ com sucesso após correção")
    void shouldReprocessDLQEventSuccessfully() {
        // GIVEN: Evento inválido na DLQ
        String sourceId = "EMP_REPROCESS_SUCCESS";
        UUID correlationId = UUID.randomUUID();
        String eventId = correlationId.toString();
        
        // 1. Publicar evento inválido
        EmployeeEventDTO invalidEvent = EmployeeEventDTO.builder()
                .eventId(eventId)
                .eventType("CREATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("999")  // ❌ Inválido
                .fullName("Reprocess Test")
                .birthDate(LocalDate.of(1990, 1, 15))
                .admissionDate(LocalDate.of(2024, 1, 10))
                .jobTitle("Analista")
                .department("TI")
                .salary(new BigDecimal("5000.00"))
                .status("ACTIVE")
                .build();

        kafkaTemplate.send("employee-create", sourceId, invalidEvent);

        // Aguardar evento ir para DLQ
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Long dlqCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.dlq_events WHERE event_id = ?",
                    Long.class, eventId);
            assertThat(dlqCount).isEqualTo(1L);
        });

        // 2. Corrigir o evento na DLQ (simular correção manual)
        Long dlqId = jdbcTemplate.queryForObject(
                "SELECT id FROM public.dlq_events WHERE event_id = ?",
                Long.class, eventId);

        String correctedPayload = """
                {
                    "eventId": "%s",
                    "eventType": "CREATE",
                    "sourceId": "%s",
                    "cpf": "11122233344",
                    "fullName": "Reprocess Test",
                    "birthDate": "1990-01-15",
                    "admissionDate": "2024-01-10",
                    "jobTitle": "Analista",
                    "department": "TI",
                    "salary": 5000.00,
                    "status": "ACTIVE"
                }
                """.formatted(eventId, sourceId);

        jdbcTemplate.update(
                "UPDATE public.dlq_events SET event_payload = ?::jsonb WHERE id = ?",
                correctedPayload, dlqId);

        // WHEN: Reprocessar via API
        await()
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    mockMvc.perform(post("/api/v1/validation/dlq/" + dlqId + "/retry")
                                    .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.success").value(true))
                            .andExpect(jsonPath("$.message").exists());
                });

        // THEN: Evento reprocessado com sucesso
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    // 1. Colaborador foi persistido
                    var employee = employeeRepository.findBySourceId(sourceId);
                    assertThat(employee).isPresent();
                    assertThat(employee.get().getCpf()).isEqualTo("11122233344");

                    // 2. Status DLQ atualizado para REPROCESSED
                    Map<String, Object> dlqEvent = jdbcTemplate.queryForMap(
                            "SELECT status, retry_count FROM public.dlq_events WHERE id = ?",
                            dlqId);
                    
                    assertThat(dlqEvent.get("status")).isEqualTo("REPROCESSED");
                    assertThat(dlqEvent.get("retry_count")).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Deve incrementar retry_count ao reprocessar evento ainda inválido")
    void shouldIncrementRetryCountOnFailedReprocessing() {
        // GIVEN: Evento inválido na DLQ
        String sourceId = "EMP_RETRY_INCREMENT";
        String eventId = UUID.randomUUID().toString();
        
        // Inserir diretamente na DLQ
        jdbcTemplate.update(
                "INSERT INTO public.dlq_events " +
                "(event_id, event_type, event_payload, error_message, retry_count, status, created_at) " +
                "VALUES (?, ?, ?::jsonb, ?, ?, ?, NOW())",
                eventId,
                "CREATE",
                "{\"sourceId\": \"" + sourceId + "\", \"cpf\": \"123\"}",  // Ainda inválido
                "Invalid CPF format",
                0,
                "PENDING"
        );

        Long dlqId = jdbcTemplate.queryForObject(
                "SELECT id FROM public.dlq_events WHERE event_id = ?",
                Long.class, eventId);

        // WHEN: Tentar reprocessar (ainda inválido)
        try {
            mockMvc.perform(post("/api/v1/validation/dlq/" + dlqId + "/retry")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            // Pode falhar, mas retry_count deve incrementar
        }

        // THEN: retry_count incrementado
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    Map<String, Object> dlqEvent = jdbcTemplate.queryForMap(
                            "SELECT retry_count, status FROM public.dlq_events WHERE id = ?",
                            dlqId);
                    
                    int retryCount = (int) dlqEvent.get("retry_count");
                    assertThat(retryCount).isGreaterThan(0);
                    
                    // Status continua PENDING (ou FAILED se excedeu limite)
                    String status = (String) dlqEvent.get("status");
                    assertThat(status).isIn("PENDING", "FAILED");
                });
    }

    @Test
    @DisplayName("Deve consultar dashboard de DLQ com estatísticas")
    void shouldQueryDLQDashboard() throws Exception {
        // GIVEN: Vários eventos na DLQ com diferentes status
        for (int i = 1; i <= 3; i++) {
            jdbcTemplate.update(
                    "INSERT INTO public.dlq_events " +
                    "(event_id, event_type, event_payload, error_message, retry_count, status, created_at) " +
                    "VALUES (?, ?, ?::jsonb, ?, ?, ?, NOW())",
                    UUID.randomUUID().toString(),
                    "CREATE",
                    "{\"sourceId\": \"EMP_DASH_" + i + "\"}",
                    "Test error",
                    i - 1,
                    i == 1 ? "PENDING" : (i == 2 ? "REPROCESSED" : "FAILED")
            );
        }

        // WHEN/THEN: Consultar dashboard
        mockMvc.perform(get("/api/v1/validation/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dlqStatistics").exists())
                .andExpect(jsonPath("$.dlqStatistics.totalEvents").exists())
                .andExpect(jsonPath("$.dlqStatistics.pendingEvents").exists())
                .andExpect(jsonPath("$.dlqStatistics.reprocessedEvents").exists())
                .andExpect(jsonPath("$.dlqStatistics.failedEvents").exists());
    }

    @Test
    @DisplayName("Deve reprocessar múltiplos eventos em lote")
    void shouldReprocessMultipleEventsInBatch() {
        // GIVEN: 3 eventos corrigidos na DLQ
        for (int i = 1; i <= 3; i++) {
            String sourceId = "EMP_BATCH_REPROCESS_" + i;
            String correctedPayload = """
                    {
                        "eventId": "%s",
                        "eventType": "CREATE",
                        "sourceId": "%s",
                        "cpf": "4445556667%d",
                        "fullName": "Batch Reprocess %d",
                        "birthDate": "1990-01-15",
                        "admissionDate": "2024-01-01",
                        "jobTitle": "Analista",
                        "department": "TI",
                        "salary": 5000.00,
                        "status": "ACTIVE"
                    }
                    """.formatted(UUID.randomUUID().toString(), sourceId, i, i);

            jdbcTemplate.update(
                    "INSERT INTO public.dlq_events " +
                    "(event_id, event_type, event_payload, error_message, retry_count, status, created_at) " +
                    "VALUES (?, ?, ?::jsonb, ?, ?, ?, NOW())",
                    UUID.randomUUID().toString(),
                    "CREATE",
                    correctedPayload,
                    "Original validation error",
                    0,
                    "PENDING"
            );
        }

        // WHEN: Reprocessar todos os eventos pendentes
        var dlqIds = jdbcTemplate.queryForList(
                "SELECT id FROM public.dlq_events " +
                "WHERE event_payload::text LIKE '%EMP_BATCH_REPROCESS_%' " +
                "AND status = 'PENDING'",
                Long.class);

        for (Long dlqId : dlqIds) {
            try {
                mockMvc.perform(post("/api/v1/validation/dlq/" + dlqId + "/retry")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
                
                // Aguardar entre reprocessamentos
                Thread.sleep(1000);
            } catch (Exception e) {
                // Log mas continua
            }
        }

        // THEN: Todos os eventos reprocessados com sucesso
        await()
                .atMost(20, SECONDS)
                .untilAsserted(() -> {
                    // Todos os 3 colaboradores foram persistidos
                    Long employeeCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM public.employees " +
                            "WHERE source_id LIKE 'EMP_BATCH_REPROCESS_%'",
                            Long.class);
                    
                    assertThat(employeeCount).isEqualTo(3L);

                    // Todos os eventos marcados como REPROCESSED
                    Long reprocessedCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM public.dlq_events " +
                            "WHERE event_payload::text LIKE '%EMP_BATCH_REPROCESS_%' " +
                            "AND status = 'REPROCESSED'",
                            Long.class);
                    
                    assertThat(reprocessedCount).isEqualTo(3L);
                });
    }

    @Test
    @DisplayName("Deve prevenir reprocessamento de evento já processado")
    void shouldPreventReprocessingAlreadyProcessedEvent() throws Exception {
        // GIVEN: Evento já marcado como REPROCESSED
        String eventId = UUID.randomUUID().toString();
        
        jdbcTemplate.update(
                "INSERT INTO public.dlq_events " +
                "(event_id, event_type, event_payload, error_message, retry_count, status, created_at) " +
                "VALUES (?, ?, ?::jsonb, ?, ?, ?, NOW())",
                eventId,
                "CREATE",
                "{\"sourceId\": \"EMP_ALREADY_PROCESSED\"}",
                "Already processed",
                1,
                "REPROCESSED"  // Já foi reprocessado
        );

        Long dlqId = jdbcTemplate.queryForObject(
                "SELECT id FROM public.dlq_events WHERE event_id = ?",
                Long.class, eventId);

        // WHEN/THEN: Tentar reprocessar novamente deve retornar erro
        mockMvc.perform(post("/api/v1/validation/dlq/" + dlqId + "/retry")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Event already reprocessed"));
    }
}
