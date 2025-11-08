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
@Table(name = "employees", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "source_id", length = 20, nullable = false, unique = true)
    private String sourceId;
    
    @Column(name = "cpf", length = 11, nullable = false, unique = true)
    private String cpf;
    
    @Column(name = "pis", length = 11)
    private String pis;
    
    @Column(name = "full_name", length = 200, nullable = false)
    private String fullName;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Column(name = "admission_date", nullable = false)
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
    
    @Column(name = "esocial_event_type", length = 10)
    private String esocialEventType;
    
    @Column(name = "esocial_sent_at")
    private LocalDateTime esocialSentAt;
    
    @Column(name = "esocial_protocol", length = 100)
    private String esocialProtocol;
    
    @Column(name = "esocial_status", length = 20)
    private String esocialStatus;
    
    @Column(name = "version")
    private Integer version;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    @Column(name = "kafka_offset")
    private Long kafkaOffset;
    
    @Column(name = "kafka_partition")
    private Integer kafkaPartition;
    
    @Column(name = "kafka_topic", length = 100)
    private String kafkaTopic;
    
    @Column(name = "correlation_id")
    private UUID correlationId;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        version = 1;
        esocialStatus = "PENDING";
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (version != null) {
            version++;
        }
    }
}
