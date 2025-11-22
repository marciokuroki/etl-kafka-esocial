package com.esocial.consumer.integration;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.model.entity.Employee;
import com.esocial.consumer.repository.EmployeeRepository;
import com.esocial.consumer.repository.EmployeeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;  
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Teste E2E: UPDATE de colaborador com versionamento
 */
@DisplayName("E2E Test: UPDATE de Colaborador com Versionamento")
class EmployeeUpdateE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeHistoryRepository employeeHistoryRepository;

    private String existingSourceId = "EMP_UPDATE_TEST";

    @BeforeEach
    void setupExistingEmployee() {
        // Criar colaborador inicial        
        Employee employee = Employee.builder()
                .sourceId(existingSourceId)
                .cpf("99988877766")
                .pis("10099887766")
                .fullName("Maria Atualização")
                .birthDate(LocalDate.of(1985, 5, 20))
                .admissionDate(LocalDate.of(2020, 1, 1))
                .jobTitle("Analista Junior")
                .department("RH")
                .salary(new BigDecimal("4000.00")) 
                .status("ACTIVE")
                .version(1)
                .build();

        employeeRepository.save(employee);
    }

    @Test
    @DisplayName("Deve atualizar colaborador e incrementar versão")
    void shouldUpdateEmployeeAndIncrementVersion() {
        UUID correlationId = UUID.randomUUID();
        // GIVEN: Evento de atualização (aumento salarial)
        EmployeeEventDTO updateEvent = EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("UPDATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(existingSourceId)
                .cpf("99988877766")
                .pis("10099887766")
                .fullName("Maria Atualização")
                .birthDate(LocalDate.of(1985, 5, 20))
                .admissionDate(LocalDate.of(2020, 1, 1))
                .jobTitle("Analista Senior")  // MUDOU
                .department("RH")
                .salary(new BigDecimal("6500.00"))  // ← BigDecimal - MUDOU
                .status("ACTIVE")
                .build();

        // WHEN: Publicar evento de UPDATE
        kafkaTemplate.send("employee-update", existingSourceId, updateEvent);

        // THEN: Validar atualização e versionamento
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    var employee = employeeRepository.findBySourceId(existingSourceId);
                    assertThat(employee).isPresent();
                    
                    // Verificar campos atualizados
                    assertThat(employee.get().getSalary())
                            .isEqualByComparingTo(new BigDecimal("6500.00"));
                    assertThat(employee.get().getJobTitle()).isEqualTo("Analista Senior");
                    
                    // Verificar versionamento
                    assertThat(employee.get().getVersion()).isEqualTo(2);

                    // Verificar histórico completo (INSERT + UPDATE)
                    var history = employeeHistoryRepository
                            .findBySourceIdOrderByChangedAtDesc(existingSourceId);
                    assertThat(history).hasSize(2);
                    
                    // Histórico mais recente = UPDATE
                    assertThat(history.get(0).getOperation()).isEqualTo("UPDATE");
                    assertThat(history.get(0).getVersion()).isEqualTo(2);
                    assertThat(history.get(0).getSalary())
                            .isEqualByComparingTo(new BigDecimal("6500.00"));
                    
                    // Histórico anterior = INSERT
                    assertThat(history.get(1).getOperation()).isEqualTo("INSERT");
                    assertThat(history.get(1).getVersion()).isEqualTo(1);
                    assertThat(history.get(1).getSalary())
                            .isEqualByComparingTo(new BigDecimal("4000.00"));
                });
    }

    @Test
    @DisplayName("Deve processar múltiplas atualizações sequenciais")
    void shouldProcessMultipleSequentialUpdates() {        
        // GIVEN: 3 atualizações sequenciais de salário
        String[] salaries = {"4500.00", "5000.00", "5500.00"};

        for (String salaryValue : salaries) {
            UUID correlationId = UUID.randomUUID();

            EmployeeEventDTO event = EmployeeEventDTO.builder()
                    .eventId(correlationId.toString())
                    .eventType("UPDATE")
                    .eventTimestamp(LocalDateTime.now())
                    .correlationId(correlationId)
                    .sourceId(existingSourceId)
                    .cpf("99988877766")
                    .fullName("Maria Atualização")
                    .birthDate(LocalDate.of(1985, 5, 20))
                    .admissionDate(LocalDate.of(2020, 1, 1))
                    .jobTitle("Analista")
                    .department("RH")
                    .salary(new BigDecimal(salaryValue))
                    .status("ACTIVE")
                    .build();

            kafkaTemplate.send("employee-update", existingSourceId, event);
            
            // Aguardar processamento antes do próximo
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
        }

        // THEN: Validar versão final e histórico completo
        await()
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    var employee = employeeRepository.findBySourceId(existingSourceId);
                    
                    // Versão final = 4 (1 INSERT + 3 UPDATEs)
                    assertThat(employee.get().getVersion()).isEqualTo(4);
                    assertThat(employee.get().getSalary())
                            .isEqualByComparingTo(new BigDecimal("5500.00"));

                    // Histórico completo
                    var history = employeeHistoryRepository
                            .findBySourceIdOrderByChangedAtDesc(existingSourceId);
                    assertThat(history).hasSize(4);
                });
    }
}
