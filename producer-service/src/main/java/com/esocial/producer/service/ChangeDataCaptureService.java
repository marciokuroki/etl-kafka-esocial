package com.esocial.producer.service;

import com.esocial.producer.model.dto.EmployeeEventDTO;
import com.esocial.producer.model.dto.EventType;
import com.esocial.producer.model.entity.Employee;
import com.esocial.producer.repository.EmployeeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final MeterRegistry meterRegistry;
    
    // Counter básico da Sprint 1
    private final Counter recordsProcessedCounter;
    
    // Métricas detalhadas da Sprint 3
    private final Counter recordsDetectedCounter;
    private final Timer cdcPollingTimer;

    @Value("${app.cdc.batch-size:100}")
    private int batchSize;

    private LocalDateTime lastProcessedTime;

    public ChangeDataCaptureService(
            EmployeeRepository employeeRepository,
            KafkaProducerService kafkaProducerService,
            MeterRegistry meterRegistry) {
        this.employeeRepository = employeeRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.meterRegistry = meterRegistry;
        
        // Counter original (Sprint 1) - retrocompatibilidade
        this.recordsProcessedCounter = Counter.builder("cdc.records.processed")
                .description("Total de registros processados pelo CDC")
                .tag("service", "producer")
                .register(meterRegistry);
        
        // Counter de registros detectados (Sprint 3)
        this.recordsDetectedCounter = Counter.builder("cdc.records.detected")
                .description("Total de mudanças detectadas pelo CDC")
                .tag("service", "producer")
                .register(meterRegistry);
        
        // Timer para latência de polling (Sprint 3)
        this.cdcPollingTimer = Timer.builder("cdc.polling.duration")
                .description("Tempo de execução do polling CDC")
                .tag("service", "producer")
                .tag("operation", "polling")
                .publishPercentiles(0.5, 0.95, 0.99) // P50, P95, P99
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

        // Medir tempo de execução do polling (Sprint 3)
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Busca colaboradores modificados desde o último processamento
            List<Employee> modifiedEmployees = employeeRepository.findModifiedAfter(lastProcessedTime);

            if (!modifiedEmployees.isEmpty()) {
                // Registrar quantidade detectada (Sprint 3)
                recordsDetectedCounter.increment(modifiedEmployees.size());
                
                log.info("Encontrados {} colaboradores modificados", modifiedEmployees.size());

                LocalDateTime currentBatchTime = LocalDateTime.now();

                for (Employee employee : modifiedEmployees) {
                    processEmployee(employee);
                }

                // Atualiza o timestamp apenas se processou com sucesso
                lastProcessedTime = currentBatchTime;
                
                // Log com métricas (Sprint 3)
                long pollingTimeMs = (long) sample.stop(cdcPollingTimer);
                log.info("Processamento concluído. {} registros em {}ms. Nova data de referência: {}", 
                        modifiedEmployees.size(), pollingTimeMs, lastProcessedTime);
            } else {
                sample.stop(cdcPollingTimer); // Parar timer mesmo sem mudanças
                log.debug("Nenhuma mudança detectada");
            }
        } catch (Exception e) {
            sample.stop(cdcPollingTimer); // Parar timer em caso de erro
            log.error("Erro ao capturar mudanças: {}", e.getMessage(), e);
            
            // Counter de erros (Sprint 3)
            Counter.builder("cdc.errors.total")
                    .description("Total de erros no CDC")
                    .tag("service", "producer")
                    .tag("error_type", e.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();
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

            // Counter básico de registros processdos
            recordsProcessedCounter.increment();
            
            // Counter detalhado por tipo de evento (Sprint 3)
            Counter.builder("cdc.records.processed.total")
                    .description("Total de registros processados pelo CDC (detalhado)")
                    .tag("service", "producer")
                    .tag("event_type", eventType.name())
                    .register(meterRegistry)
                    .increment();

            log.debug("Colaborador processado: id={}, type={}", employee.getEmployeeId(), eventType);
        } catch (Exception e) {
            log.error("Erro ao processar colaborador id={}: {}",
                    employee.getEmployeeId(), e.getMessage(), e);
            
            // Counter de falhas no processamento (Sprint 3)
            Counter.builder("cdc.processing.failures")
                    .description("Falhas ao processar registros individuais")
                    .tag("service", "producer")
                    .register(meterRegistry)
                    .increment();
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
