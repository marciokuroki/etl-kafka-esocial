package com.esocial.producer.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    
    private String eventId;
    private EventType eventType;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventTimestamp;
    
    // Dados do colaborador
    private String employeeId;
    private String cpf;
    private String pis;
    private String fullName;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate admissionDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate terminationDate;
    
    private String jobTitle;
    private String department;
    private BigDecimal salary;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // Metadados
    private String sourceSystem;
    private UUID correlationId;
    
    public static EmployeeEventDTO createEvent(
            String employeeId, String cpf, String fullName, 
            LocalDate admissionDate, String department, BigDecimal salary) {
        return EmployeeEventDTO.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EventType.CREATE)
                .eventTimestamp(LocalDateTime.now())
                .employeeId(employeeId)
                .cpf(cpf)
                .fullName(fullName)
                .admissionDate(admissionDate)
                .department(department)
                .salary(salary)
                .status("ACTIVE")
                .sourceSystem("HR_SYSTEM")
                .correlationId(UUID.randomUUID())
                .build();
    }
}
