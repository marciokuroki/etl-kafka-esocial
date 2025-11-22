package com.esocial.consumer.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEventDTO {
    
    @JsonProperty("source_id")
    private String sourceId; 
    
    @JsonProperty("event_id")
    private String eventId; // ID do evento Kafka
    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("event_timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventTimestamp;
    
    @JsonProperty("correlation_id")
    private UUID correlationId; // Rastreabilidade
    
    // --- CPF e Documentos
    @JsonProperty("cpf")
    private String cpf;
    
    @JsonProperty("pis")
    private String pis;
    
    @JsonProperty("ctps")
    private String ctps;
    
    @JsonProperty("matricula")
    private String matricula;
    
    // --- Dados Pessoais
    @JsonProperty("full_name")
    private String fullName;
    
    @JsonProperty("birth_date")
    private LocalDate birthDate;
    
    @JsonProperty("sex")
    private String sex;
    
    @JsonProperty("nationality")
    private String nationality;
    
    @JsonProperty("marital_status")
    private String maritalStatus;
    
    @JsonProperty("race")
    private String race;
    
    @JsonProperty("education_level")
    private String educationLevel;
    
    @JsonProperty("disability")
    private String disability;
    
    // --- Contato
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("phone")
    private String phone;
    
    @JsonProperty("zip_code")
    private String zipCode;
    
    @JsonProperty("uf")
    private String uf;
    
    // --- Laborais
    @JsonProperty("admission_date")
    private LocalDate admissionDate;
    
    @JsonProperty("termination_date")
    private LocalDate terminationDate;
    
    @JsonProperty("job_title")
    private String jobTitle;
    
    @JsonProperty("department")
    private String department;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("contract_type")
    private String contractType;
    
    @JsonProperty("cbo")
    private String cbo;
    
    @JsonProperty("salary")
    private BigDecimal salary;
    
    // --- Status
    @JsonProperty("status")
    private String status; // ACTIVE, INACTIVE
    
    // --- Kafka Metadata
    @JsonProperty("kafka_offset")
    private Long kafkaOffset;
    
    @JsonProperty("kafka_partition")
    private Integer kafkaPartition;
    
    @JsonProperty("kafka_topic")
    private String kafkaTopic;
    
    // --- Getters auxiliares (opcional, já que @Data gera tudo)
    public boolean isSourceIdValid() {
        return sourceId != null && !sourceId.trim().isEmpty();
    }
    
    public boolean isCpfValid() {
        return cpf != null && cpf.matches("\\d{11}");
    }
}
