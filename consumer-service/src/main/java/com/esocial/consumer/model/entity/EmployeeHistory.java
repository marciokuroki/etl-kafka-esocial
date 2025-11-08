package com.esocial.consumer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employees_history", schema = "audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;
    
    @Column(name = "employee_id")
    private Long employeeId;
    
    @Column(name = "source_id", length = 20, nullable = false)
    private String sourceId;
    
    @Column(name = "cpf", length = 11, nullable = false)
    private String cpf;
    
    @Column(name = "pis", length = 11)
    private String pis;
    
    @Column(name = "full_name", length = 200)
    private String fullName;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Column(name = "admission_date")
    private LocalDate admissionDate;
    
    @Column(name = "termination_date")
    private LocalDate terminationDate;
    
    @Column(name = "job_title", length = 100)
    private String jobTitle;
    
    @Column(name = "department", length = 100)
    private String department;
    
    @Column(name = "salary", precision = 10, scale = 2)
    private BigDecimal salary;
    
    @Column(name = "status", length = 20)
    private String status;
    
    @Column(name = "version", nullable = false)
    private Integer version;
    
    @Column(name = "operation", length = 10, nullable = false)
    private String operation;
    
    @Column(name = "changed_at")
    private LocalDateTime changedAt;
    
    @Column(name = "changed_by", length = 50)
    private String changedBy;
    
    @Column(name = "kafka_offset")
    private Long kafkaOffset;
    
    @Column(name = "correlation_id")
    private UUID correlationId;
    
    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
