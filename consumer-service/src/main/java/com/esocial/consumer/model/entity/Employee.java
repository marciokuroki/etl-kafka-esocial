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
@Table(name = "employees", schema = "public", indexes = {
        @Index(name = "idx_cpf", columnList = "cpf", unique = true),
        @Index(name = "idx_pis", columnList = "pis"),
        @Index(name = "idx_source_id", columnList = "source_id", unique = true),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_esocial_status", columnList = "esocial_status"),
        @Index(name = "idx_ctps", columnList = "ctps"),
        @Index(name = "idx_matricula", columnList = "matricula")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // --- Identificação
    @Column(name = "source_id", length = 20, nullable = false, unique = true)
    private String sourceId; // ID único do sistema origem
    
    @Column(name = "cpf", length = 11, nullable = false, unique = true)
    private String cpf;
    
    @Column(name = "pis", length = 11)
    private String pis;
    
    @Column(name = "ctps", length = 20, unique = true)
    private String ctps;
    
    @Column(name = "matricula", length = 20, nullable = false)
    private String matricula;
    
    // --- Dados Pessoais
    @Column(name = "full_name", length = 200, nullable = false)
    private String fullName;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Column(name = "sex", length = 1)
    private String sex;
    
    @Column(name = "nationality", length = 1)
    private String nationality;
    
    @Column(name = "marital_status", length = 1)
    private String maritalStatus;
    
    @Column(name = "race", length = 2)
    private String race;
    
    @Column(name = "education_level", length = 2)
    private String educationLevel;
    
    @Column(name = "disability", length = 2)
    private String disability;
    
    // --- Dados de Contato
    @Column(name = "email", length = 150)
    private String email;
    
    @Column(name = "phone", length = 15)
    private String phone;
    
    @Column(name = "zip_code", length = 8)
    private String zipCode;
    
    @Column(name = "uf", length = 2)
    private String uf;
    
    // --- Dados Laborais
    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;
    
    @Column(name = "termination_date")
    private LocalDate terminationDate;
    
    @Column(name = "job_title", length = 100)
    private String jobTitle;
    
    @Column(name = "department", length = 100)
    private String department;
    
    @Column(name = "category", length = 3)
    private String category;
    
    @Column(name = "contract_type", length = 3)
    private String contractType;
    
    @Column(name = "cbo", length = 6)
    private String cbo;
    
    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;
    
    // --- Status Interno
    @Column(name = "status", length = 20)
    private String status; // ACTIVE, INACTIVE, SUSPENDED
    
    // --- Integração eSocial
    @Column(name = "esocial_event_type", length = 10)
    private String esocialEventType; // S-2200, S-2300, etc
    
    @Column(name = "esocial_sent_at")
    private LocalDateTime esocialSentAt;
    
    @Column(name = "esocial_protocol", length = 100)
    private String esocialProtocol;
    
    @Column(name = "esocial_status", length = 20)
    private String esocialStatus; // PENDING, SENT, RECEIVED, REJECTED, PROCESSED
    
    // --- Rastreabilidade Kafka
    @Column(name = "kafka_offset")
    private Long kafkaOffset;
    
    @Column(name = "kafka_partition")
    private Integer kafkaPartition;
    
    @Column(name = "kafka_topic", length = 100)
    private String kafkaTopic;
    
    @Column(name = "correlation_id")
    private UUID correlationId;
    
    // --- Auditoria
    @Column(name = "version")
    private Integer version;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    // --- Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        version = 1;
        esocialStatus = "PENDING";
        if (status == null) {
            status = "ACTIVE";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (version != null) {
            version++;
        }
    }
}
