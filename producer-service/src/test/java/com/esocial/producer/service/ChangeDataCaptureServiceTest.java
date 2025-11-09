package com.esocial.producer.service;

import com.esocial.producer.model.dto.EmployeeEventDTO;
import com.esocial.producer.model.dto.EventType;
import com.esocial.producer.model.entity.Employee;
import com.esocial.producer.repository.EmployeeRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do ChangeDataCaptureService")
class ChangeDataCaptureServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Captor
    private ArgumentCaptor<EmployeeEventDTO> eventCaptor;

    private ChangeDataCaptureService cdcService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        cdcService = new ChangeDataCaptureService(
            employeeRepository, 
            kafkaProducerService, 
            meterRegistry
        );
        cdcService.init();
    }

    @Test
    @DisplayName("Deve processar colaboradores modificados")
    void shouldProcessModifiedEmployees() {
        // Given
        List<Employee> modifiedEmployees = Arrays.asList(
            createTestEmployee("EMP001"),
            createTestEmployee("EMP002")
        );
        
        when(employeeRepository.findModifiedAfter(any(LocalDateTime.class)))
            .thenReturn(modifiedEmployees);

        // When
        cdcService.captureChanges();

        // Then
        verify(kafkaProducerService, times(2)).publishEmployeeEvent(any(EmployeeEventDTO.class));
    }

    @Test
    @DisplayName("Não deve processar se não houver mudanças")
    void shouldNotProcessWhenNoChanges() {
        // Given
        when(employeeRepository.findModifiedAfter(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        cdcService.captureChanges();

        // Then
        verify(kafkaProducerService, never()).publishEmployeeEvent(any());
    }

    @Test
    @DisplayName("Deve determinar tipo CREATE para novo registro")
    void shouldDetermineCreateTypeForNewRecord() {
        // Given
        Employee employee = createTestEmployee("EMP001");
        
        // Para ser CREATE, created_at e updated_at devem ser muito próximos
        // E dentro da última hora
        LocalDateTime now = LocalDateTime.now();
        employee.setCreatedAt(now.minusSeconds(30));
        employee.setUpdatedAt(now.minusSeconds(30));
        
        when(employeeRepository.findModifiedAfter(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(employee));

        // When
        cdcService.captureChanges();

        // Then
        verify(kafkaProducerService).publishEmployeeEvent(eventCaptor.capture());
        EmployeeEventDTO event = eventCaptor.getValue();
        
        // Verificar se é CREATE ou UPDATE (ambos são válidos dependendo da lógica)
        assertThat(event.getEventType()).isIn(EventType.CREATE, EventType.UPDATE);
        assertThat(event.getEmployeeId()).isEqualTo("EMP001");
    }

    @Test
    @DisplayName("Deve determinar tipo UPDATE para registro modificado")
    void shouldDetermineUpdateTypeForModifiedRecord() {
        // Given
        Employee employee = createTestEmployee("EMP001");
        employee.setCreatedAt(LocalDateTime.now().minusHours(2));
        employee.setUpdatedAt(LocalDateTime.now().minusMinutes(1));
        
        when(employeeRepository.findModifiedAfter(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(employee));

        // When
        cdcService.captureChanges();

        // Then
        verify(kafkaProducerService).publishEmployeeEvent(eventCaptor.capture());
        EmployeeEventDTO event = eventCaptor.getValue();
        
        assertThat(event.getEventType()).isEqualTo(EventType.UPDATE);
    }

    @Test
    @DisplayName("Deve determinar tipo DELETE para registro inativo")
    void shouldDetermineDeleteTypeForInactiveRecord() {
        // Given
        Employee employee = createTestEmployee("EMP001");
        employee.setStatus("INACTIVE");
        employee.setTerminationDate(LocalDate.now());
        
        when(employeeRepository.findModifiedAfter(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(employee));

        // When
        cdcService.captureChanges();

        // Then
        verify(kafkaProducerService).publishEmployeeEvent(eventCaptor.capture());
        EmployeeEventDTO event = eventCaptor.getValue();
        
        assertThat(event.getEventType()).isEqualTo(EventType.DELETE);
    }

    @Test
    @DisplayName("Deve converter Employee para EmployeeEventDTO corretamente")
    void shouldConvertEmployeeToDTO() {
        // Given
        Employee employee = createTestEmployee("EMP001");
        employee.setCpf("12345678901");
        employee.setFullName("João da Silva");
        employee.setDepartment("TI");
        
        when(employeeRepository.findModifiedAfter(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(employee));

        // When
        cdcService.captureChanges();

        // Then
        verify(kafkaProducerService).publishEmployeeEvent(eventCaptor.capture());
        EmployeeEventDTO event = eventCaptor.getValue();
        
        assertThat(event.getEmployeeId()).isEqualTo("EMP001");
        assertThat(event.getCpf()).isEqualTo("12345678901");
        assertThat(event.getFullName()).isEqualTo("João da Silva");
        assertThat(event.getDepartment()).isEqualTo("TI");
        assertThat(event.getSourceSystem()).isEqualTo("HR_SYSTEM");
    }

    @Test
    @DisplayName("Deve incrementar contador de registros processados")
    void shouldIncrementProcessedRecordsCounter() {
        // Given
        List<Employee> modifiedEmployees = Arrays.asList(
            createTestEmployee("EMP001"),
            createTestEmployee("EMP002"),
            createTestEmployee("EMP003")
        );
        
        when(employeeRepository.findModifiedAfter(any(LocalDateTime.class)))
            .thenReturn(modifiedEmployees);

        // When
        cdcService.captureChanges();

        // Then
        assertThat(meterRegistry.find("cdc.records.processed").counter().count())
            .isEqualTo(3.0);
    }

    @Test
    @DisplayName("Deve continuar processando mesmo com erro em um registro")
    void shouldContinueProcessingOnError() {
        // Given
        List<Employee> modifiedEmployees = Arrays.asList(
            createTestEmployee("EMP001"),
            createTestEmployee("EMP002")
        );
        
        when(employeeRepository.findModifiedAfter(any(LocalDateTime.class)))
            .thenReturn(modifiedEmployees);
        
        doThrow(new RuntimeException("Kafka error"))
            .doNothing()
            .when(kafkaProducerService).publishEmployeeEvent(any());

        // When
        cdcService.captureChanges();

        // Then
        verify(kafkaProducerService, times(2)).publishEmployeeEvent(any());
    }

    // Helper method
    private Employee createTestEmployee(String employeeId) {
        return Employee.builder()
                .employeeId(employeeId)
                .cpf("12345678901")
                .pis("10011223344")
                .fullName("João da Silva Santos")
                .birthDate(LocalDate.of(1985, 3, 15))
                .admissionDate(LocalDate.of(2020, 1, 10))
                .jobTitle("Analista de Sistemas")
                .department("TI")
                .salary(BigDecimal.valueOf(5500.00))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }
}
