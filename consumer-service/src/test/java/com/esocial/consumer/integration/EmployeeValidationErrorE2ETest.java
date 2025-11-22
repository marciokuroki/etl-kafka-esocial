package com.esocial.consumer.integration;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Teste E2E: Validações com erro
 * 
 * Cenários testados:
 * 1. CPF inválido (formato incorreto)
 * 2. Data de nascimento futura
 * 3. Salário abaixo do mínimo
 * 4. Múltiplas validações falhando
 * 5. Erros registrados corretamente
 * 6. Eventos vão para DLQ
 */
@DisplayName("E2E Test: Validações com Erro")
class EmployeeValidationErrorE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Deve rejeitar colaborador com CPF inválido")
    void shouldRejectEmployeeWithInvalidCpf() {
        UUID correlationId = UUID.randomUUID();
        // GIVEN: Evento com CPF inválido (menos de 11 dígitos)
        String sourceId = "EMP_INVALID_CPF";
        
        EmployeeEventDTO event = EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("CREATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("123")  // ❌ INVÁLIDO: menos de 11 dígitos
                .pis("10011223344")
                .fullName("João CPF Inválido")
                .birthDate(LocalDate.of(1990, 1, 15))
                .admissionDate(LocalDate.of(2024, 1, 10))
                .jobTitle("Analista")
                .department("TI")
                .salary(new BigDecimal("5000.00"))
                .status("ACTIVE")
                .build();

        // WHEN: Publicar evento
        kafkaTemplate.send("employee-create", sourceId, event);

        // THEN: Validar rejeição
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    // 1. Colaborador NÃO foi persistido
                    var employee = employeeRepository.findBySourceId(sourceId);
                    assertThat(employee).isEmpty();

                    // 2. Erro de validação registrado
                    List<Map<String, Object>> errors = jdbcTemplate.queryForList(
                            "SELECT validation_rule, error_message, severity " +
                            "FROM public.validation_errors " +
                            "WHERE source_id = ?",
                            sourceId
                    );
                    
                    assertThat(errors).isNotEmpty();
                    assertThat(errors.get(0).get("validation_rule"))
                            .toString().contains("CPF");
                    assertThat(errors.get(0).get("severity"))
                            .isEqualTo("ERROR");

                    // 3. Evento na DLQ
                    Long dlqCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM public.dlq_events " +
                            "WHERE event_payload::text LIKE ?",
                            Long.class,
                            "%" + sourceId + "%"
                    );
                    assertThat(dlqCount).isGreaterThan(0);
                });
    }

    @Test
    @DisplayName("Deve rejeitar colaborador com data de nascimento futura")
    void shouldRejectEmployeeWithFutureBirthDate() {
        UUID correlationId = UUID.randomUUID();
        // GIVEN: Evento com data de nascimento no futuro
        String sourceId = "EMP_FUTURE_BIRTH";
        
        EmployeeEventDTO event = EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("CREATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("11122233344")
                .pis("10011223344")
                .fullName("Maria Futuro")
                .birthDate(LocalDate.now().plusYears(1))  // ❌ Data futura
                .admissionDate(LocalDate.of(2024, 1, 10))
                .jobTitle("Analista")
                .department("TI")
                .salary(new BigDecimal("5000.00"))
                .status("ACTIVE")
                .build();

        // WHEN: Publicar evento
        kafkaTemplate.send("employee-create", sourceId, event);

        // THEN: Validar rejeição
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    // Colaborador NÃO persistido
                    var employee = employeeRepository.findBySourceId(sourceId);
                    assertThat(employee).isEmpty();

                    // Erro específico de data de nascimento
                    List<Map<String, Object>> errors = jdbcTemplate.queryForList(
                            "SELECT validation_rule, error_message " +
                            "FROM public.validation_errors " +
                            "WHERE source_id = ? AND validation_rule LIKE '%BIRTH_DATE%'",
                            sourceId
                    );
                    
                    assertThat(errors).isNotEmpty();
                    assertThat(errors.get(0).get("error_message").toString())
                            .containsIgnoringCase("futura");
                });
    }

    @Test
    @DisplayName("Deve rejeitar colaborador com salário abaixo do mínimo")
    void shouldRejectEmployeeWithBelowMinimumSalary() {
        UUID correlationId = UUID.randomUUID();
        // GIVEN: Evento com salário muito baixo
        String sourceId = "EMP_LOW_SALARY";
        
        EmployeeEventDTO event = EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("CREATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("22233344455")
                .pis("10011223344")
                .fullName("José Salário Baixo")
                .birthDate(LocalDate.of(1990, 1, 15))
                .admissionDate(LocalDate.of(2024, 1, 10))
                .jobTitle("Analista")
                .department("TI")
                .salary(new BigDecimal("500.00"))  // ❌ Abaixo do mínimo
                .status("ACTIVE")
                .build();

        // WHEN: Publicar evento
        kafkaTemplate.send("employee-create", sourceId, event);

        // THEN: Validar rejeição
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    var employee = employeeRepository.findBySourceId(sourceId);
                    assertThat(employee).isEmpty();

                    List<Map<String, Object>> errors = jdbcTemplate.queryForList(
                            "SELECT validation_rule, error_message " +
                            "FROM public.validation_errors " +
                            "WHERE source_id = ? AND validation_rule LIKE '%SALARY%'",
                            sourceId
                    );
                    
                    assertThat(errors).isNotEmpty();
                    assertThat(errors.get(0).get("error_message").toString())
                            .containsIgnoringCase("mínimo");
                });
    }

    @Test
    @DisplayName("Deve registrar múltiplos erros de validação em cascata")
    void shouldRegisterMultipleValidationErrors() {
        UUID correlationId = UUID.randomUUID();
        // GIVEN: Evento com MÚLTIPLOS erros
        String sourceId = "EMP_MULTIPLE_ERRORS";
        
        EmployeeEventDTO event = EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("CREATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("123")  // ❌ ERRO 1: CPF inválido
                .pis(null)   // ❌ ERRO 2: PIS obrigatório
                .fullName(null)  // ❌ ERRO 3: Nome obrigatório
                .birthDate(LocalDate.now().plusYears(1))  // ❌ ERRO 4: Data futura
                .admissionDate(LocalDate.of(2024, 1, 10))
                .jobTitle("Analista")
                .department("TI")
                .salary(new BigDecimal("100.00"))  // ❌ ERRO 5: Salário baixo
                .status("ACTIVE")
                .build();

        // WHEN: Publicar evento
        kafkaTemplate.send("employee-create", sourceId, event);

        // THEN: Validar que TODOS os erros foram registrados
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    // Colaborador NÃO persistido
                    var employee = employeeRepository.findBySourceId(sourceId);
                    assertThat(employee).isEmpty();

                    // MÚLTIPLOS erros registrados
                    List<Map<String, Object>> errors = jdbcTemplate.queryForList(
                            "SELECT validation_rule, error_message, severity " +
                            "FROM public.validation_errors " +
                            "WHERE source_id = ? " +
                            "ORDER BY created_at",
                            sourceId
                    );
                    
                    // Deve ter pelo menos 3 erros críticos
                    assertThat(errors).hasSizeGreaterThanOrEqualTo(3);
                    
                    // Verificar que erros cobrem diferentes regras
                    List<String> rules = errors.stream()
                            .map(e -> e.get("validation_rule").toString())
                            .toList();
                    
                    assertThat(rules).contains(
                            "INVALID_CPF_FORMAT",
                            "INVALID_BIRTH_DATE",
                            "INVALID_SALARY"
                    );

                    // Pelo menos um erro com severidade ERROR
                    long errorCount = errors.stream()
                            .filter(e -> "ERROR".equals(e.get("severity")))
                            .count();
                    assertThat(errorCount).isGreaterThan(0);
                });
    }

    @Test
    @DisplayName("Deve permitir warnings mas rejeitar errors")
    void shouldAllowWarningsButRejectErrors() {
        UUID correlationId = UUID.randomUUID();
        // GIVEN: Evento com WARNING (não crítico) mas sem ERRORs
        String sourceId = "EMP_WITH_WARNING";
        
        EmployeeEventDTO event = EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("CREATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("33344455566")  // ✅ Válido
                .pis("10011223344")
                .fullName("Pedro Warning")
                .birthDate(LocalDate.of(1960, 1, 15))  // ⚠️ WARNING: > 60 anos
                .admissionDate(LocalDate.of(2024, 1, 10))
                .jobTitle("Analista")
                .department("TI")
                .salary(new BigDecimal("5000.00"))  // ✅ Válido
                .status("ACTIVE")
                .build();

        // WHEN: Publicar evento
        kafkaTemplate.send("employee-create", sourceId, event);

        // THEN: Deve ser processado APESAR do warning
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    // Colaborador FOI persistido (warning não bloqueia)
                    var employee = employeeRepository.findBySourceId(sourceId);
                    assertThat(employee).isPresent();

                    // Warning foi registrado
                    List<Map<String, Object>> warnings = jdbcTemplate.queryForList(
                            "SELECT validation_rule, severity " +
                            "FROM public.validation_errors " +
                            "WHERE source_id = ? AND severity = 'WARNING'",
                            sourceId
                    );
                    
                    assertThat(warnings).isNotEmpty();
                    assertThat(warnings.get(0).get("validation_rule").toString())
                            .containsIgnoringCase("AGE");
                });
    }
}
