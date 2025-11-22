package com.esocial.consumer.integration;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.model.entity.Employee;
import com.esocial.consumer.repository.EmployeeRepository;
import com.esocial.consumer.repository.EmployeeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Teste E2E: DELETE de colaborador com soft delete
 * 
 * Fluxo testado:
 * 1. Colaborador existente no sistema
 * 2. Evento DELETE publicado no Kafka
 * 3. Consumer processa evento
 * 4. Soft delete: status alterado para TERMINATED
 * 5. Registro permanece no banco (não é deletado fisicamente)
 * 6. Histórico registra operação DELETE
 * 7. Versionamento incrementado
 */
@DisplayName("E2E Test: DELETE de Colaborador com Soft Delete")
class EmployeeDeleteE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeHistoryRepository employeeHistoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String existingSourceId = "EMP_DELETE_TEST";

    @BeforeEach
    void setupExistingEmployee() {
        // Criar colaborador ativo para ser deletado
        Employee employee = Employee.builder()
                .sourceId(existingSourceId)
                .cpf("88877766655")
                .pis("10088877766")
                .fullName("Pedro Delete Teste")
                .birthDate(LocalDate.of(1988, 8, 10))
                .admissionDate(LocalDate.of(2021, 3, 15))
                .jobTitle("Coordenador")
                .department("Financeiro")
                .salary(new BigDecimal("7000.00"))
                .status("ACTIVE")
                .esocialStatus("SENT")
                .version(1)
                .build();

        employeeRepository.save(employee);
    }

    @Test
    @DisplayName("Deve processar DELETE e aplicar soft delete")
    void shouldProcessDeleteAndApplySoftDelete() {
        UUID correlationId = UUID.randomUUID();
        // GIVEN: Evento de DELETE (desligamento)
        EmployeeEventDTO deleteEvent = EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("DELETE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(existingSourceId)
                .cpf("88877766655")
                .pis("10088877766")
                .fullName("Pedro Delete Teste")
                .birthDate(LocalDate.of(1988, 8, 10))
                .admissionDate(LocalDate.of(2021, 3, 15))
                .terminationDate(LocalDate.now())  // Data de desligamento
                .jobTitle("Coordenador")
                .department("Financeiro")
                .salary(new BigDecimal("7000.00"))
                .status("TERMINATED")  // Status alterado
                .build();

        // WHEN: Publicar evento de DELETE
        kafkaTemplate.send("employee-delete", existingSourceId, deleteEvent);

        // THEN: Validar soft delete
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    var employee = employeeRepository.findBySourceId(existingSourceId);
                    
                    // 1. Registro AINDA EXISTE (não foi deletado fisicamente)
                    assertThat(employee).isPresent();
                    
                    // 2. Status alterado para TERMINATED
                    assertThat(employee.get().getStatus()).isEqualTo("TERMINATED");
                    
                    // 3. Data de término preenchida
                    assertThat(employee.get().getTerminationDate()).isNotNull();
                    assertThat(employee.get().getTerminationDate()).isEqualTo(LocalDate.now());
                    
                    // 4. Versionamento incrementado
                    assertThat(employee.get().getVersion()).isEqualTo(2);
                    
                    // 5. Status eSocial alterado para PENDING (precisa notificar desligamento)
                    assertThat(employee.get().getEsocialStatus()).isEqualTo("PENDING");

                    // 6. Verificar histórico (INSERT + DELETE)
                    var history = employeeHistoryRepository
                            .findBySourceIdOrderByChangedAtDesc(existingSourceId);
                    assertThat(history).hasSize(2);
                    
                    // Mais recente = DELETE
                    assertThat(history.get(0).getOperation()).isEqualTo("DELETE");
                    assertThat(history.get(0).getVersion()).isEqualTo(2);
                    
                    // Anterior = INSERT
                    assertThat(history.get(1).getOperation()).isEqualTo("INSERT");
                    assertThat(history.get(1).getVersion()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Deve rejeitar DELETE de colaborador inexistente")
    void shouldRejectDeleteOfNonExistentEmployee() {
        UUID correlationId = UUID.randomUUID();
        // GIVEN: Evento de DELETE para colaborador que NÃO existe
        String nonExistentId = "EMP_DOES_NOT_EXIST";
        
        EmployeeEventDTO deleteEvent = EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("DELETE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(nonExistentId)
                .cpf("99999999999")
                .fullName("Inexistente")
                .birthDate(LocalDate.of(1990, 1, 1))
                .admissionDate(LocalDate.of(2020, 1, 1))
                .terminationDate(LocalDate.now())
                .jobTitle("Teste")
                .department("Teste")
                .salary(new BigDecimal("5000.00"))
                .status("TERMINATED")
                .build();

        // WHEN: Publicar evento
        kafkaTemplate.send("employee-delete", nonExistentId, deleteEvent);

        // THEN: Evento deve ir para DLQ
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    // 1. Colaborador NÃO existe
                    var employee = employeeRepository.findBySourceId(nonExistentId);
                    assertThat(employee).isEmpty();

                    // 2. Erro de validação registrado
                    Long errorCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM public.validation_errors " +
                            "WHERE source_id = ? AND validation_rule LIKE '%NOT_FOUND%'",
                            Long.class,
                            nonExistentId
                    );
                    assertThat(errorCount).isGreaterThan(0);

                    // 3. Evento na DLQ
                    Long dlqCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM public.dlq_events " +
                            "WHERE event_payload::text LIKE ?",
                            Long.class,
                            "%" + nonExistentId + "%"
                    );
                    assertThat(dlqCount).isGreaterThan(0);
                });
    }

    @Test
    @DisplayName("Deve manter dados após DELETE para auditoria")
    void shouldKeepDataAfterDeleteForAudit() {
        // GIVEN: Colaborador com histórico completo
        String sourceId = "EMP_AUDIT_TEST";
        
        // Criar colaborador
        Employee employee = Employee.builder()
                .sourceId(sourceId)
                .cpf("77766655544")
                .pis("10077766655")
                .fullName("Ana Auditoria")
                .birthDate(LocalDate.of(1992, 5, 20))
                .admissionDate(LocalDate.of(2020, 1, 1))
                .jobTitle("Analista")
                .department("Auditoria")
                .salary(new BigDecimal("5500.00"))
                .status("ACTIVE")
                .version(1)
                .build();
        employeeRepository.save(employee);

        // Atualizar (simular histórico)
        employee.setSalary(new BigDecimal("6000.00"));
        employee.setVersion(2);
        employeeRepository.save(employee);

        // WHEN: DELETE
        UUID correlationId = UUID.randomUUID();
        EmployeeEventDTO deleteEvent = EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("DELETE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("77766655544")
                .fullName("Ana Auditoria")
                .birthDate(LocalDate.of(1992, 5, 20))
                .admissionDate(LocalDate.of(2020, 1, 1))
                .terminationDate(LocalDate.now())
                .jobTitle("Analista")
                .department("Auditoria")
                .salary(new BigDecimal("6000.00"))
                .status("TERMINATED")
                .build();

        kafkaTemplate.send("employee-delete", sourceId, deleteEvent);

        // THEN: Todos os dados e histórico preservados
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    // Registro principal ainda existe
                    var emp = employeeRepository.findBySourceId(sourceId);
                    assertThat(emp).isPresent();
                    assertThat(emp.get().getStatus()).isEqualTo("TERMINATED");
                    
                    // Todos os campos preservados
                    assertThat(emp.get().getCpf()).isEqualTo("77766655544");
                    assertThat(emp.get().getFullName()).isEqualTo("Ana Auditoria");
                    assertThat(emp.get().getSalary())
                            .isEqualByComparingTo(new BigDecimal("6000.00"));
                    
                    // Histórico completo preservado
                    var history = employeeHistoryRepository
                            .findBySourceIdOrderByChangedAtDesc(sourceId);
                    assertThat(history).hasSize(3);  // INSERT + UPDATE + DELETE
                    
                    // Verificar todas as operações no histórico
                    assertThat(history.get(0).getOperation()).isEqualTo("DELETE");
                    assertThat(history.get(1).getOperation()).isEqualTo("UPDATE");
                    assertThat(history.get(2).getOperation()).isEqualTo("INSERT");
                });
    }
}
