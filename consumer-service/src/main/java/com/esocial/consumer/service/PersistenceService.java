package com.esocial.consumer.service;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.model.entity.Employee;
import com.esocial.consumer.model.entity.EmployeeHistory;
import com.esocial.consumer.repository.EmployeeHistoryRepository;
import com.esocial.consumer.repository.EmployeeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class PersistenceService {
    
    private final EmployeeRepository employeeRepository;
    private final EmployeeHistoryRepository employeeHistoryRepository;
    private final Counter employeesCreatedCounter;
    private final Counter employeesUpdatedCounter;
    private final Counter employeesDeletedCounter;
    
    public PersistenceService(
            EmployeeRepository employeeRepository,
            EmployeeHistoryRepository employeeHistoryRepository,
            MeterRegistry meterRegistry) {
        this.employeeRepository = employeeRepository;
        this.employeeHistoryRepository = employeeHistoryRepository;
        this.employeesCreatedCounter = Counter.builder("employees.created")
                .description("Total de colaboradores criados")
                .tag("service", "consumer")
                .register(meterRegistry);
        this.employeesUpdatedCounter = Counter.builder("employees.updated")
                .description("Total de colaboradores atualizados")
                .tag("service", "consumer")
                .register(meterRegistry);
        this.employeesDeletedCounter = Counter.builder("employees.deleted")
                .description("Total de colaboradores deletados")
                .tag("service", "consumer")
                .register(meterRegistry);
    }
    
    /**
     * Persiste um evento de colaborador no banco de dados
     */
    @Transactional
    public void persistEvent(EmployeeEventDTO event, Long kafkaOffset, Integer kafkaPartition, String kafkaTopic) {
        log.debug("Persistindo evento: {} (tipo: {})", event.getEventId(), event.getEventType());
        
        switch (event.getEventType()) {
            case "S-2300", "S-2306" -> { 
                // S-2300: Admissão de Trabalhador
                // S-2306: Admissão de Aprendiz
                log.info("CREATE: Processando admissão - sourceId={}", event.getSourceId());
                createEmployee(event, kafkaOffset, kafkaPartition, kafkaTopic);
            }
            
            case "S-2400", "S-2405", "S-2410" -> { 
                // S-2400: Alterações Contratuais
                // S-2405: Alteração de Aprendiz
                // S-2410: Remuneração
                log.info("UPDATE: Processando alteração - sourceId={}", event.getSourceId());
                updateEmployee(event, kafkaOffset, kafkaPartition, kafkaTopic);
            }
            
            case "S-2420", "S-3000" -> { 
                // S-2420: Desligamento de Trabalhador
                // S-3000: Exclusão de Evento
                log.info("DELETE: Processando desligamento - sourceId={}", event.getSourceId());
                deleteEmployee(event, kafkaOffset, kafkaPartition, kafkaTopic);
            }
            
            default -> {
                log.warn("Tipo de evento não suportado: {}", event.getEventType());
            }
        }
    }
    
    private void createEmployee(EmployeeEventDTO event, Long kafkaOffset, Integer kafkaPartition, String kafkaTopic) {
        // Verificar se já existe
        if (employeeRepository.existsBySourceId(event.getSourceId())) {
            log.warn("Colaborador com sourceId {} já existe. Atualizando ao invés de criar.", 
                    event.getSourceId());
            updateEmployee(event, kafkaOffset, kafkaPartition, kafkaTopic);
            return;
        }
        
        Employee employee = convertDTOToEntity(event, kafkaOffset, kafkaPartition, kafkaTopic);
        Employee saved = employeeRepository.save(employee);
        
        // Criar registro de histórico
        createHistoryRecord(saved, "INSERT");
        
        employeesCreatedCounter.increment();
        log.info("Colaborador criado: sourceId={}, id={}", saved.getSourceId(), saved.getId());
    }
    
    private void updateEmployee(EmployeeEventDTO event, Long kafkaOffset, Integer kafkaPartition, String kafkaTopic) {
        Optional<Employee> existingOpt = employeeRepository.findBySourceId(event.getSourceId());
        
        if (existingOpt.isEmpty()) {
            log.warn("Colaborador com sourceId {} não encontrado para atualização. Criando novo registro.", 
                    event.getSourceId());
            createEmployee(event, kafkaOffset, kafkaPartition, kafkaTopic);
            return;
        }
        
        Employee existing = existingOpt.get();
        updateEntityFromDTO(existing, event, kafkaOffset, kafkaPartition, kafkaTopic);
        Employee saved = employeeRepository.save(existing);
        
        // Criar registro de histórico
        createHistoryRecord(saved, "UPDATE");
        
        employeesUpdatedCounter.increment();
        log.info("Colaborador atualizado: sourceId={}, id={}, version={}", 
                saved.getSourceId(), saved.getId(), saved.getVersion());
    }
    
    private void deleteEmployee(EmployeeEventDTO event, Long kafkaOffset, Integer kafkaPartition, String kafkaTopic) {
        Optional<Employee> existingOpt = employeeRepository.findBySourceId(event.getSourceId());
        
        if (existingOpt.isEmpty()) {
            log.warn("Colaborador com sourceId {} não encontrado para deleção.", event.getSourceId());
            return;
        }
        
        Employee existing = existingOpt.get();
        
        // Soft delete: atualizar status e data de demissão
        existing.setStatus("INACTIVE");
        existing.setTerminationDate(event.getTerminationDate());
        existing.setKafkaOffset(kafkaOffset);
        existing.setKafkaPartition(kafkaPartition);
        existing.setKafkaTopic(kafkaTopic);
        
        Employee saved = employeeRepository.save(existing);
        
        // Criar registro de histórico
        createHistoryRecord(saved, "DELETE");
        
        employeesDeletedCounter.increment();
        log.info("Colaborador deletado (soft delete): sourceId={}, id={}", 
                saved.getSourceId(), saved.getId());
    }
    
    private Employee convertDTOToEntity(EmployeeEventDTO dto, Long kafkaOffset, Integer kafkaPartition, String kafkaTopic) {
        return Employee.builder()
                .sourceId(dto.getSourceId())
                .cpf(dto.getCpf())
                .pis(dto.getPis())
                .fullName(dto.getFullName())
                .birthDate(dto.getBirthDate())
                .admissionDate(dto.getAdmissionDate())
                .terminationDate(dto.getTerminationDate())
                .jobTitle(dto.getJobTitle())
                .department(dto.getDepartment())
                .salary(dto.getSalary())
                .status(dto.getStatus())
                .createdBy("system")
                .updatedBy("system")
                .kafkaOffset(kafkaOffset)
                .kafkaPartition(kafkaPartition)
                .kafkaTopic(kafkaTopic)
                .correlationId(dto.getCorrelationId())
                .build();
    }
    
    private void updateEntityFromDTO(Employee entity, EmployeeEventDTO dto, 
                                     Long kafkaOffset, Integer kafkaPartition, String kafkaTopic) {
        entity.setCpf(dto.getCpf());
        entity.setPis(dto.getPis());
        entity.setFullName(dto.getFullName());
        entity.setBirthDate(dto.getBirthDate());
        entity.setAdmissionDate(dto.getAdmissionDate());
        entity.setTerminationDate(dto.getTerminationDate());
        entity.setJobTitle(dto.getJobTitle());
        entity.setDepartment(dto.getDepartment());
        entity.setSalary(dto.getSalary());
        entity.setStatus(dto.getStatus());
        entity.setUpdatedBy("system");
        entity.setKafkaOffset(kafkaOffset);
        entity.setKafkaPartition(kafkaPartition);
        entity.setKafkaTopic(kafkaTopic);
        entity.setCorrelationId(dto.getCorrelationId());
    }
    
    private void createHistoryRecord(Employee employee, String operation) {
        EmployeeHistory history = EmployeeHistory.builder()
                .employeeId(employee.getId())
                .sourceId(employee.getSourceId())
                .cpf(employee.getCpf())
                .pis(employee.getPis())
                .fullName(employee.getFullName())
                .birthDate(employee.getBirthDate())
                .admissionDate(employee.getAdmissionDate())
                .terminationDate(employee.getTerminationDate())
                .jobTitle(employee.getJobTitle())
                .department(employee.getDepartment())
                .salary(employee.getSalary())
                .status(employee.getStatus())
                .version(employee.getVersion())
                .operation(operation)
                .changedBy("system")
                .kafkaOffset(employee.getKafkaOffset())
                .correlationId(employee.getCorrelationId())
                .build();
        
        employeeHistoryRepository.save(history);
        log.debug("Registro de histórico criado: operation={}, employeeId={}", operation, employee.getId());
    }
}
