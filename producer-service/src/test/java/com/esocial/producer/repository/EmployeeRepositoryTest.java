package com.esocial.producer.repository;

import com.esocial.producer.model.entity.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Testes do EmployeeRepository")
class EmployeeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("Deve encontrar colaboradores modificados após determinada data")
    void shouldFindModifiedAfter() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        
        Employee oldEmployee = createTestEmployee("EMP001");
        oldEmployee.setUpdatedAt(cutoffTime.minusHours(2));
        
        Employee newEmployee = createTestEmployee("EMP002");
        newEmployee.setUpdatedAt(cutoffTime.plusMinutes(30));
        
        entityManager.persist(oldEmployee);
        entityManager.persist(newEmployee);
        entityManager.flush();

        // When
        List<Employee> result = employeeRepository.findModifiedAfter(cutoffTime);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmployeeId()).isEqualTo("EMP002");
    }

    @Test
    @DisplayName("Deve encontrar colaboradores criados após determinada data")
    void shouldFindCreatedAfter() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        
        Employee oldEmployee = createTestEmployee("EMP001");
        oldEmployee.setCreatedAt(cutoffTime.minusHours(2));
        
        Employee newEmployee = createTestEmployee("EMP002");
        newEmployee.setCreatedAt(cutoffTime.plusMinutes(30));
        
        entityManager.persist(oldEmployee);
        entityManager.persist(newEmployee);
        entityManager.flush();

        // When
        List<Employee> result = employeeRepository.findCreatedAfter(cutoffTime);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmployeeId()).isEqualTo("EMP002");
    }

    @Test
    @DisplayName("Deve ordenar resultados por updated_at ascendente")
    void shouldOrderByUpdatedAtAscending() {
        // Given
        LocalDateTime baseTime = LocalDateTime.now().minusHours(1);
        
        Employee emp1 = createTestEmployee("EMP001");
        emp1.setUpdatedAt(baseTime.plusMinutes(30));
        
        Employee emp2 = createTestEmployee("EMP002");
        emp2.setUpdatedAt(baseTime.plusMinutes(10));
        
        Employee emp3 = createTestEmployee("EMP003");
        emp3.setUpdatedAt(baseTime.plusMinutes(20));
        
        entityManager.persist(emp1);
        entityManager.persist(emp2);
        entityManager.persist(emp3);
        entityManager.flush();

        // When
        List<Employee> result = employeeRepository.findModifiedAfter(baseTime);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getEmployeeId()).isEqualTo("EMP002");
        assertThat(result.get(1).getEmployeeId()).isEqualTo("EMP003");
        assertThat(result.get(2).getEmployeeId()).isEqualTo("EMP001");
    }

    private Employee createTestEmployee(String employeeId) {
        // Gerar CPF único baseado no employeeId
        int hash = Math.abs(employeeId.hashCode()) % 100000000; // 8 dígitos
        String cpf = String.format("%08d%03d", hash, 
                                Integer.parseInt(employeeId.replaceAll("\\D", "")));
    
        // Garantir que tenha exatamente 11 dígitos
        if (cpf.length() > 11) {
            cpf = cpf.substring(0, 11);
        } else if (cpf.length() < 11) {
            cpf = String.format("%011d", Long.parseLong(cpf));
        }
    
        return Employee.builder()
            .employeeId(employeeId)
            .cpf(cpf)
            .fullName("Test Employee")
            .admissionDate(LocalDate.now())
            .department("TI")
            .salary(BigDecimal.valueOf(5000))
            .status("ACTIVE")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
