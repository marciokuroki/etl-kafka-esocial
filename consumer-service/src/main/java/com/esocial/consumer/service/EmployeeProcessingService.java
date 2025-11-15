package com.esocial.consumer.service;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.model.entity.Employee;
import com.esocial.consumer.model.entity.EmployeeEvent;
import com.esocial.consumer.repository.EmployeeRepository;
import com.esocial.consumer.repository.EmployeeEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional
public class EmployeeProcessingService {
    
    private final EmployeeRepository employeeRepository;
    private final EmployeeEventRepository eventRepository;
    
    public EmployeeProcessingService(EmployeeRepository employeeRepository,
                                     EmployeeEventRepository eventRepository) {
        this.employeeRepository = employeeRepository;
        this.eventRepository = eventRepository;
    }
    
    /**
     * Processa um evento de collaborador
     * Detecta duplicação, cria ou atualiza registro
     */
    public Employee processEvent(EmployeeEventDTO event, EmployeeEvent eventRecord) {
        log.info("Processando evento: sourceId={}, eventType={}", 
            event.getSourceId(), event.getEventType());
        
        // 1. Verificar se já existe
        if (employeeRepository.existsBySourceId(event.getSourceId())) {
            log.warn("Evento já processado: sourceId={}", event.getSourceId());
            
            // Buscar e atualizar registro existente
            return employeeRepository.findBySourceId(event.getSourceId())
                .map(existing -> updateExistingEmployee(existing, event))
                .orElseThrow(() -> new RuntimeException("Colaborador não encontrado: " + event.getSourceId()));
        }
        
        // 2. Criar novo registro
        log.info("Criando novo colaborador: sourceId={}, cpf={}", 
            event.getSourceId(), maskCpf(event.getCpf()));
        
        Employee employee = Employee.builder()
            .sourceId(event.getSourceId())
            .cpf(event.getCpf())
            .pis(event.getPis())
            .ctps(event.getCtps())
            .matricula(event.getMatricula())
            .fullName(event.getFullName())
            .birthDate(event.getBirthDate())
            .admissionDate(event.getAdmissionDate())
            .terminationDate(event.getTerminationDate())
            .email(event.getEmail())
            .phone(event.getPhone())
            .sex(event.getSex())
            .salary(event.getSalary())
            .status(event.getStatus() != null ? event.getStatus() : "ACTIVE")
            .jobTitle(event.getJobTitle())
            .department(event.getDepartment())
            .esocialEventType(event.getEventType())
            .esocialStatus("PENDING")
            .kafkaOffset(event.getKafkaOffset())
            .kafkaPartition(event.getKafkaPartition())
            .kafkaTopic(event.getKafkaTopic())
            .correlationId(event.getCorrelationId())
            .createdBy("KAFKA-CONSUMER")
            .build();
        
        Employee saved = employeeRepository.save(employee);
        log.info("Colaborador criado com sucesso: id={}, sourceId={}", 
            saved.getId(), saved.getSourceId());
        
        // 3. Atualizar referência no evento
        eventRecord.setEmployeeId(saved.getId());
        eventRepository.save(eventRecord);
        
        return saved;
    }
    
    /**
     * Atualiza um colaborador existente
     */
    private Employee updateExistingEmployee(Employee existing, EmployeeEventDTO event) {
        log.info("Atualizando colaborador existente: id={}, sourceId={}", 
            existing.getId(), existing.getSourceId());
        
        existing.setAdmissionDate(event.getAdmissionDate());
        existing.setTerminationDate(event.getTerminationDate());
        existing.setSalary(event.getSalary());
        existing.setJobTitle(event.getJobTitle());
        existing.setDepartment(event.getDepartment());
        existing.setStatus(event.getStatus() != null ? event.getStatus() : existing.getStatus());
        existing.setEsocialEventType(event.getEventType());
        existing.setUpdatedBy("KAFKA-CONSUMER");
        existing.setUpdatedAt(LocalDateTime.now());
        
        Employee updated = employeeRepository.save(existing);
        log.info("Colaborador atualizado com sucesso: id={}", updated.getId());
        
        return updated;
    }
    
    /**
     * Mascara CPF para logs (segurança)
     */
    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 4) return "***";
        return "***" + cpf.substring(cpf.length() - 4);
    }
}
