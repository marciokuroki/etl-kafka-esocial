package com.esocial.consumer.repository;

import com.esocial.consumer.model.entity.ValidationError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ValidationErrorRepository extends JpaRepository<ValidationError, Long> {
    
    List<ValidationError> findByEventId(String eventId);
    
    List<ValidationError> findBySeverity(String severity);
    
    @Query("SELECT v FROM ValidationError v WHERE v.createdAt > :since ORDER BY v.createdAt DESC")
    List<ValidationError> findRecentErrors(LocalDateTime since);
    
    @Query("SELECT v.validationRule, COUNT(v) FROM ValidationError v GROUP BY v.validationRule ORDER BY COUNT(v) DESC")
    List<Object[]> countErrorsByRule();
}
