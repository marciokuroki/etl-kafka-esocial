package com.esocial.consumer.integration;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Teste E2E: INSERT de colaborador válido
 * 
 * Fluxo testado:
 * 1. Producer captura INSERT no PostgreSQL origem
 * 2. Evento publicado no Kafka (topic: employee-create)
 * 3. Consumer consome evento
 * 4. Validações estruturais e de negócio passam
 * 5. Registro persistido no PostgreSQL destino
 * 6. Histórico criado na tabela de audit
 */
@DisplayName("E2E Test: INSERT de Colaborador Válido")
class EmployeeInsertE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Deve processar INSERT de colaborador válido end-to-end")
    void shouldProcessValidEmployeeInsertE2E() {
        // GIVEN: Evento de criação de colaborador válido
        String sourceId = "EMP_TEST_001";
        EmployeeEventDTO event = createValidInsertEvent(sourceId, toBigDecimal("5500.00"));

        // WHEN: Publicar evento no Kafka
        kafkaTemplate.send("employee-create", sourceId, event);

        // THEN
        await().atMost(10, SECONDS).untilAsserted(() -> {
            var employee = employeeRepository.findBySourceId(sourceId);
            assertThat(employee).isPresent();
            assertThat(employee.get().getSalary())
                    .isEqualByComparingTo(new BigDecimal("5500.00"));
        });
    }

    @Test
    @DisplayName("Deve processar múltiplos INSERTs simultaneamente")
    void shouldProcessMultipleInsertsSimultaneously() {
        // GIVEN: 5 eventos de criação simultâneos
        for (int i = 1; i <= 5; i++) {
            String sourceId = "EMP_BATCH_00" + i;
            BigDecimal valor = new BigDecimal(5000.00).add(new BigDecimal(i * 100));
            EmployeeEventDTO event = createValidInsertEvent(sourceId, toBigDecimal(valor.toPlainString()));

            kafkaTemplate.send("employee-create", sourceId, event);
        }

        // THEN: Aguardar processamento e validar
        await()
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    Long count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM public.employees WHERE source_id LIKE 'EMP_BATCH_%'",
                            Long.class
                    );
                    assertThat(count).isEqualTo(5L);

                    // Validar histórico completo
                    Long historyCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM audit.employees_history WHERE source_id LIKE 'EMP_BATCH_%'",
                            Long.class
                    );
                    assertThat(historyCount).isEqualTo(5L);
                });
    }
}
