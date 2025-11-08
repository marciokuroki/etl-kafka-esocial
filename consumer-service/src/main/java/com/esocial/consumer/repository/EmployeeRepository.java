package com.esocial.consumer.repository;

import com.esocial.consumer.model.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    Optional<Employee> findBySourceId(String sourceId);
    
    Optional<Employee> findByCpf(String cpf);
    
    boolean existsBySourceId(String sourceId);
    
    boolean existsByCpf(String cpf);
}
