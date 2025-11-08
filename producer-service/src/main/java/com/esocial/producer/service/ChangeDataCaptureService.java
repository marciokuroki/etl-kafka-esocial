package com.esocial.producer.service;

import com.esocial.producer.model.dto.EmployeeEventDTO;
import com.esocial.producer.model.dto.EventType;
import com.esocial.producer.model.entity.Employee;
import com.esocial.producer.repository.EmployeeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ChangeDataCaptureService {

    private final EmployeeRepository employeeRepository;
    private final KafkaProducerService kafkaProducerService;
    private final Counter recordsProcessedCounter;

    @Value("${app.cdc.batch-size:100}")
    private int batchSize;

    private LocalDateTime lastProcessedTime;

    public ChangeDataCaptureService(
            EmployeeRepository employeeRepository,
            KafkaProducerService kafkaProducerService,
            MeterRegistry meterRegistry) {
        this.employeeRepository = employeeRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.recordsProcessedCounter = Counter.builder("cdc.records.processed")
                .description("Total de registros processados pelo CDC")
                .tag("service", "producer")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        // Inicializa com timestamp atual - 1 hora para capturar mudanças recentes
        this.lastProcessedTime = LocalDateTime.now().minusHours(1);
        log.info("CDC Service inicializado. Última data processada: {}", lastProcessedTime);
    }

    /**
     * Polling periódico para detectar mudanças
     * Executa a cada 5 segundos (configurável)
     */
    @Scheduled(fixedDelayString = "${app.cdc.polling-interval:5000}")
    @Transactional(readOnly = true)
    public void captureChanges() {
        log.debug("Iniciando captura de mudanças. Última data processada: {}", lastProcessedTime);

        try {
            // Busca colaboradores modificados desde o último processamento
            List<Employee> modifiedEmployees = employeeRepository.findModifiedAfter(lastProcessedTime);

            if (!modifiedEmployees.isEmpty()) {
                log.info("Encontrados {} colaboradores modificados", modifiedEmployees.size());

                LocalDateTime currentBatchTime = LocalDateTime.now();

                for (Employee employee : modifiedEmployees) {
                    processEmployee(employee);
                }

                // Atualiza o timestamp apenas se processou com sucesso
                lastProcessedTime = currentBatchTime;
                log.info("Processamento concluído. Nova data de referência: {}", lastProcessedTime);
            } else {
                log.debug("Nenhuma mudança detectada");
            }
        } catch (Exception e) {
            log.error("Erro ao capturar mudanças: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa um colaborador e publica evento no Kafka
     */
    private void processEmployee(Employee employee) {
        try {
            // Determina o tipo de evento baseado em lógica de negócio
            EventType eventType = determineEventType(employee);

            // Converte entidade para DTO
            EmployeeEventDTO event = convertToDTO(employee, eventType);

            // Publica no Kafka
            kafkaProducerService.publishEmployeeEvent(event);

            // Incrementa contador
            recordsProcessedCounter.increment();

            log.debug("Colaborador processado: id={}, type={}", employee.getEmployeeId(), eventType);
        } catch (Exception e) {
            log.error("Erro ao processar colaborador id={}: {}",
                    employee.getEmployeeId(), e.getMessage(), e);
        }
    }

    /**
     * Determina o tipo de evento baseado em regras de negócio
     */
    private EventType determineEventType(Employee employee) {
        // Simplificado: verifica se é criação recente ou atualização
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        if (employee.getCreatedAt() != null &&
                employee.getCreatedAt().isAfter(oneHourAgo) &&
                employee.getCreatedAt().equals(employee.getUpdatedAt())) {
            return EventType.CREATE;
        }

        if ("INACTIVE".equals(employee.getStatus()) || employee.getTerminationDate() != null) {
            return EventType.DELETE; // Soft delete
        }

        return EventType.UPDATE;
    }

    /**
     * Converte Employee para EmployeeEventDTO
     */
    private EmployeeEventDTO convertToDTO(Employee employee, EventType eventType) {
        return EmployeeEventDTO.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventTimestamp(LocalDateTime.now())
                .employeeId(employee.getEmployeeId())
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
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .sourceSystem("HR_SYSTEM")
                .correlationId(UUID.randomUUID())
                .build();
    }
}
