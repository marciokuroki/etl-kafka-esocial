package com.esocial.consumer.service;

import com.esocial.consumer.model.dto.EmployeeEventDTO;
import com.esocial.consumer.model.entity.Employee;
import com.esocial.consumer.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class EmployeeEventPersistenceService {
    
    private final EmployeeRepository employeeRepository;
    
    public EmployeeEventPersistenceService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }
    
    /**
     * Persiste evento baseado no tipo eSocial
     * S-2300 (Admissão) → CREATE
     * S-2400 (Alteração) → UPDATE
     * S-2420 (Desligamento) → DELETE
     */
    @Transactional
    public void persistEvent(EmployeeEventDTO event, Long kafkaOffset, 
                            Integer kafkaPartition, String kafkaTopic) {
        
        if (event == null || event.getEventType() == null) {
            log.error("Evento inválido: dto={}, eventType={}", event, 
                event != null ? event.getEventType() : null);
            return;
        }
        
        log.info("Persistindo evento: sourceId={}, eventType={}, eventId={}", 
            event.getSourceId(), event.getEventType(), event.getEventId());
        
        // Switch baseado no tipo eSocial
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
    
    /**
     * CREATE: Criar novo colaborador (Admissão S-2300/S-2306)
     */
    @Transactional
    private void createEmployee(EmployeeEventDTO event, Long kafkaOffset, 
                               Integer kafkaPartition, String kafkaTopic) {
        try {
            log.debug("CREATE iniciado: sourceId={}, cpf={}", 
                event.getSourceId(), maskCpf(event.getCpf()));
            
            // 1. Verificar duplicação
            if (employeeRepository.existsBySourceId(event.getSourceId())) {
                log.warn("CREATE: Colaborador já existe na base - sourceId={}", 
                    event.getSourceId());
                return;
            }
            
            // 2. Verificar CPF duplicado (outro collaborador)
            if (event.getCpf() != null && employeeRepository.existsByCpf(event.getCpf())) {
                log.error("CREATE: CPF já cadastrado - cpf={}", maskCpf(event.getCpf()));
                return;
            }
            
            // 3. Criar novo colaborador
            Employee employee = Employee.builder()
                .sourceId(event.getSourceId())
                .cpf(event.getCpf())
                .pis(event.getPis())
                .ctps(event.getCtps())
                .matricula(event.getMatricula())
                .fullName(event.getFullName())
                .birthDate(event.getBirthDate())
                .admissionDate(event.getAdmissionDate())
                .email(event.getEmail())
                .phone(event.getPhone())
                .sex(event.getSex())
                .salary(event.getSalary())
                .status("ACTIVE")
                .jobTitle(event.getJobTitle())
                .department(event.getDepartment())
                .category(event.getCategory())
                .contractType(event.getContractType())
                .cbo(event.getCbo())
                .uf(event.getUf())
                .nationality(event.getNationality())
                .maritalStatus(event.getMaritalStatus())
                .race(event.getRace())
                .educationLevel(event.getEducationLevel())
                .disability(event.getDisability())
                .esocialEventType(event.getEventType())
                .esocialStatus("PENDING")
                .kafkaOffset(kafkaOffset)
                .kafkaPartition(kafkaPartition)
                .kafkaTopic(kafkaTopic)
                .correlationId(event.getCorrelationId())
                .createdBy("KAFKA-CONSUMER")
                .build();
            
            Employee saved = employeeRepository.save(employee);
            log.info("CREATE concluído com sucesso: id={}, sourceId={}, cpf={}", 
                saved.getId(), saved.getSourceId(), maskCpf(saved.getCpf()));
            
        } catch (Exception e) {
            log.error("Erro ao criar colaborador: sourceId={}, erro={}", 
                event.getSourceId(), e.getMessage(), e);
            throw new RuntimeException("Erro ao criar colaborador", e);
        }
    }
    
    /**
     * UPDATE: Atualizar dados do colaborador (Alteração S-2400/S-2405/S-2410)
     */
    @Transactional
    private void updateEmployee(EmployeeEventDTO event, Long kafkaOffset, 
                               Integer kafkaPartition, String kafkaTopic) {
        try {
            log.debug("UPDATE iniciado: sourceId={}", event.getSourceId());
            
            // 1. Buscar colaborador existente
            Optional<Employee> existingOpt = employeeRepository.findBySourceId(event.getSourceId());
            
            if (existingOpt.isEmpty()) {
                log.warn("UPDATE: Colaborador não encontrado - sourceId={}", event.getSourceId());
                return;
            }
            
            Employee existing = existingOpt.get();
            
            // 2. Atualizar campos alteráveis
            existing.setJobTitle(event.getJobTitle());
            existing.setDepartment(event.getDepartment());
            existing.setSalary(event.getSalary());
            existing.setEmail(event.getEmail());
            existing.setPhone(event.getPhone());
            existing.setCategory(event.getCategory());
            existing.setContractType(event.getContractType());
            existing.setCbo(event.getCbo());
            existing.setEsocialEventType(event.getEventType());
            existing.setUpdatedBy("KAFKA-CONSUMER");
            existing.setUpdatedAt(LocalDateTime.now());
            existing.setVersion((existing.getVersion() != null ? existing.getVersion() : 0) + 1);
            
            // 3. Salvar alterações
            Employee updated = employeeRepository.save(existing);
            log.info("UPDATE concluído com sucesso: id={}, sourceId={}, version={}", 
                updated.getId(), updated.getSourceId(), updated.getVersion());
            
        } catch (Exception e) {
            log.error("Erro ao atualizar colaborador: sourceId={}, erro={}", 
                event.getSourceId(), e.getMessage(), e);
            throw new RuntimeException("Erro ao atualizar colaborador", e);
        }
    }
    
    /**
     * DELETE: Marcar como desligado (Desligamento S-2420/S-3000)
     * Nota: Não deleta fisicamente, apenas marca como INACTIVE
     */
    @Transactional
    private void deleteEmployee(EmployeeEventDTO event, Long kafkaOffset, 
                               Integer kafkaPartition, String kafkaTopic) {
        try {
            log.debug("DELETE iniciado: sourceId={}", event.getSourceId());
            
            // 1. Buscar colaborador existente
            Optional<Employee> existingOpt = employeeRepository.findBySourceId(event.getSourceId());
            
            if (existingOpt.isEmpty()) {
                log.warn("DELETE: Colaborador não encontrado - sourceId={}", event.getSourceId());
                return;
            }
            
            Employee existing = existingOpt.get();
            
            // 2. Verificar se já está inativo
            if ("INACTIVE".equals(existing.getStatus())) {
                log.warn("DELETE: Colaborador já inativo - sourceId={}", event.getSourceId());
                return;
            }
            
            // 3. Marcar como inativo (soft delete)
            existing.setStatus("INACTIVE");
            existing.setTerminationDate(event.getTerminationDate());
            existing.setEsocialEventType(event.getEventType());
            existing.setUpdatedBy("KAFKA-CONSUMER");
            existing.setUpdatedAt(LocalDateTime.now());
            existing.setVersion((existing.getVersion() != null ? existing.getVersion() : 0) + 1);
            
            // 4. Salvar alterações
            Employee updated = employeeRepository.save(existing);
            log.info("DELETE concluído com sucesso: id={}, sourceId={}, status=INACTIVE, terminationDate={}", 
                updated.getId(), updated.getSourceId(), updated.getTerminationDate());
            
        } catch (Exception e) {
            log.error("Erro ao desligar colaborador: sourceId={}, erro={}", 
                event.getSourceId(), e.getMessage(), e);
            throw new RuntimeException("Erro ao desligar colaborador", e);
        }
    }
    
    /**
     * Mascara CPF para segurança em logs
     */
    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 4) {
            return "***";
        }
        return "***" + cpf.substring(cpf.length() - 4);
    }
}
