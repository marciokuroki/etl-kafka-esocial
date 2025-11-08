package com.esocial.producer.repository;

import com.esocial.producer.model.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    
    /**
     * Busca colaboradores modificados após determinada data/hora
     * Simula CDC (Change Data Capture)
     */
    @Query("SELECT e FROM Employee e WHERE e.updatedAt > :lastProcessedTime ORDER BY e.updatedAt ASC")
    List<Employee> findModifiedAfter(LocalDateTime lastProcessedTime);
    
    /**
     * Busca colaboradores criados após determinada data/hora
     */
    @Query("SELECT e FROM Employee e WHERE e.createdAt > :lastProcessedTime ORDER BY e.createdAt ASC")
    List<Employee> findCreatedAfter(LocalDateTime lastProcessedTime);
}
