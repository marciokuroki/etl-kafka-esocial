package com.esocial.consumer.repository;

import com.esocial.consumer.model.entity.EmployeeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeHistoryRepository extends JpaRepository<EmployeeHistory, Long> {
    
    List<EmployeeHistory> findByEmployeeIdOrderByChangedAtDesc(Long employeeId);
    
    List<EmployeeHistory> findBySourceIdOrderByChangedAtDesc(String sourceId);
}
