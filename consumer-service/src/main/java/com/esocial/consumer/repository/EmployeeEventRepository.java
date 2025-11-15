package com.esocial.consumer.repository;

import com.esocial.consumer.model.entity.EmployeeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmployeeEventRepository extends JpaRepository<EmployeeEvent, String> {

    /**
     * Verifica se um evento com o ID especificado já foi processado
     * Usado para VI-005: EventIdUniquenessValidationRule
     */
    @Query("SELECT COUNT(e) > 0 FROM EmployeeEvent e WHERE e.eventId = :eventId")
    boolean existsById(@Param("eventId") String eventId);

    /**
     * Busca evento pelo ID
     */
    Optional<EmployeeEvent> findByEventId(String eventId);

    Optional<EmployeeEvent> findBySourceId(String sourceId);

    /**
     * Conta quantos eventos foram processados em um período
     */
    @Query("SELECT COUNT(e) FROM EmployeeEvent e WHERE e.processedAt BETWEEN :startDate AND :endDate")
    long countByProcessedAtBetween(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);

    /**
     * Busca eventos com erro
     */
    @Query("SELECT e FROM EmployeeEvent e WHERE e.status = 'ERROR' ORDER BY e.processedAt DESC")
    java.util.List<EmployeeEvent> findAllErrors();

    /**
     * Conta eventos por status
     */
    @Query("SELECT COUNT(e) FROM EmployeeEvent e WHERE e.status = :status")
    long countByStatus(@Param("status") String status);
}
