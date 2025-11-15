package com.esocial.consumer.service;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.model.entity.Employee;
import com.esocial.consumer.model.entity.EmployeeEvent;
import com.esocial.consumer.repository.EmployeeRepository;
import com.esocial.consumer.repository.EmployeeEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeProcessingServiceTest {
    
    @Mock
    private EmployeeRepository employeeRepository;
    
    @Mock
    private EmployeeEventRepository eventRepository;
    
    @InjectMocks
    private EmployeeProcessingService service;
    
    private EmployeeEventDTO eventDTO;
    private EmployeeEvent eventRecord;
    
    @BeforeEach
    public void setUp() {
        eventDTO = EmployeeEventDTO.builder()
            .sourceId("HR-SYSTEM-001-12345")
            .eventId("evt-20251110-001")
            .eventType("S-2300")
            .cpf("12345678901")
            .fullName("João Silva")
            .birthDate(LocalDate.of(1990, 5, 15))
            .admissionDate(LocalDate.of(2020, 1, 10))
            .salary(new BigDecimal("5000.00"))
            .status("ACTIVE")
            .correlationId(UUID.randomUUID())
            .kafkaTopic("employee.events")
            .kafkaPartition(0)
            .kafkaOffset(100L)
            .build();
        
        eventRecord = EmployeeEvent.builder()
            .sourceId(eventDTO.getSourceId())
            .eventId(eventDTO.getEventId())
            .build();
    }
    
    @Test
    public void shouldCreateNewEmployeeWhenNotExists() {
        // Given
        when(employeeRepository.existsBySourceId(eventDTO.getSourceId())).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(1L);
            return emp;
        });
        
        // When
        Employee result = service.processEvent(eventDTO, eventRecord);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSourceId()).isEqualTo(eventDTO.getSourceId());
        assertThat(result.getCpf()).isEqualTo(eventDTO.getCpf());
        
        verify(employeeRepository).save(any(Employee.class));
        verify(employeeRepository).existsBySourceId(eventDTO.getSourceId());
    }
    
    @Test
    public void shouldUpdateExistingEmployeeWhenExists() {
        // Given
        Employee existingEmployee = Employee.builder()
            .id(1L)
            .sourceId(eventDTO.getSourceId())
            .cpf(eventDTO.getCpf())
            .salary(new BigDecimal("4000.00"))
            .build();
        
        when(employeeRepository.existsBySourceId(eventDTO.getSourceId())).thenReturn(true);
        when(employeeRepository.findBySourceId(eventDTO.getSourceId()))
            .thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(existingEmployee);
        
        // When
        Employee result = service.processEvent(eventDTO, eventRecord);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        
        verify(employeeRepository).existsBySourceId(eventDTO.getSourceId());
        verify(employeeRepository).findBySourceId(eventDTO.getSourceId());
        verify(employeeRepository).save(any(Employee.class));
    }
}
