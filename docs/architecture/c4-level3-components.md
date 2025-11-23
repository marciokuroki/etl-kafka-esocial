# C4 Model - Level 3: Componentes

**Versão:** 1.0  
**Data:** 2025-11-22  
**Autor:** Márcio Kuroki Gonçalves  
**Projeto:** Pipeline ETL eSocial

---

## Índice

1. [Visão Geral](#visão-geral)
2. [Producer Service - Componentes](#producer-service---componentes)
3. [Consumer Service - Componentes](#consumer-service---componentes)
4. [Diagramas PlantUML](#diagramas-plantuml)
5. [Interações Entre Componentes](#interações-entre-componentes)
6. [Decisões de Design](#decisões-de-design)

---

## Visão Geral

Este documento detalha a **arquitetura interna** dos microsserviços Producer e Consumer, identificando todos os componentes, suas responsabilidades e interações.

### Hierarquia C4

```

Level 1: Sistema Context          ← Sistema no ecossistema
↓
Level 2: Containers               ← Microsserviços e infraestrutura
↓
Level 3: Componentes (ESTE DOC)   ← Componentes internos dos serviços
↓
Level 4: Código                   ← Classes e interfaces

```

---

## Producer Service - Componentes

### Visão Geral da Estrutura

```

producer-service/
├── src/main/java/com/esocial/producer/
│   ├── config/                    ← Configurações Spring
│   │   ├── KafkaProducerConfig.java
│   │   └── DataSourceConfig.java
│   ├── service/                   ← Lógica de negócio
│   │   ├── ChangeDataCaptureService.java     (CDC Layer)
│   │   ├── KafkaProducerService.java         (Kafka Layer)
│   │   └── EventMappingService.java          (Event Layer)
│   ├── model/                     ← Entidades e DTOs
│   │   ├── entity/
│   │   │   └── Employee.java
│   │   └── dto/
│   │       └── EmployeeEventDTO.java
│   ├── repository/                ← Persistência
│   │   └── EmployeeRepository.java
│   └── scheduler/                 ← Agendamento CDC
│       └── CdcPollingScheduler.java

```

---

### Componente 1: CDC Layer (Change Data Capture)

#### **ChangeDataCaptureService**

**Responsabilidade:** Capturar mudanças no banco de origem via polling periódico

**Tecnologias:**
- Spring Boot `@Scheduled`
- JDBC Template
- HikariCP Connection Pool

**Fluxo de Execução:**

```

@Service
@Slf4j
public class ChangeDataCaptureService {

    private final EmployeeRepository sourceRepository;
    private final EventMappingService eventMapper;
    private final KafkaProducerService kafkaProducer;
    private LocalDateTime lastProcessedTime;
    
    /**
     * Executa a cada 5 segundos
     * Fixed delay: aguarda término antes de próxima execução
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void captureChanges() {
        log.info("CDC: Iniciando captura desde {}", lastProcessedTime);
        
        // 1. Query incremental
        List<Employee> modifiedEmployees = sourceRepository
            .findModifiedAfter(lastProcessedTime);
        
        log.info("CDC: {} registros modificados encontrados", 
                 modifiedEmployees.size());
        
        // 2. Processar cada mudança
        for (Employee employee : modifiedEmployees) {
            try {
                // 2.1 Determinar tipo de evento
                EventType eventType = determineEventType(employee);
                
                // 2.2 Mapear para DTO
                EmployeeEventDTO event = eventMapper.toDTO(employee, eventType);
                
                // 2.3 Publicar no Kafka
                kafkaProducer.publish(event);
                
                // 2.4 Atualizar offset
                lastProcessedTime = employee.getUpdatedAt();
                
            } catch (Exception e) {
                log.error("CDC: Erro ao processar employee {}", 
                         employee.getEmployeeId(), e);
                // Continua processando próximos registros
            }
        }
        
        log.info("CDC: Captura concluída. Próximo offset: {}", lastProcessedTime);
    }
    
    /**
     * Lógica para determinar tipo de evento
     */
    private EventType determineEventType(Employee employee) {
        if (employee.getCreatedAt().equals(employee.getUpdatedAt())) {
            return EventType.CREATE;
        } else if (employee.getStatus().equals("TERMINATED")) {
            return EventType.DELETE;
        } else {
            return EventType.UPDATE;
        }
    }
    }

```

**Métricas Coletadas:**
```

// Prometheus metrics
@Timed(value = "cdc.polling.duration", description = "Tempo de polling CDC")
@Counted(value = "cdc.records.processed", description = "Registros processados")

```

**Configuração:**
```


# application.yml

cdc:
polling-interval-ms: 5000
batch-size: 100
initial-delay-ms: 10000

```

---

#### **OffsetManager**

**Responsabilidade:** Gerenciar último timestamp processado (offset)

```

@Component
public class OffsetManager {

    private final AtomicReference<LocalDateTime> lastOffset = 
        new AtomicReference<>(LocalDateTime.now().minusDays(1));
    
    public LocalDateTime getLastOffset() {
        return lastOffset.get();
    }
    
    public void updateOffset(LocalDateTime newOffset) {
        lastOffset.set(newOffset);
        // Persistir em arquivo ou DB para sobreviver restarts
        persistOffset(newOffset);
    }
    
    private void persistOffset(LocalDateTime offset) {
        // TODO: Implementar persistência (arquivo ou tabela de controle)
    }
    }

```

---

### Componente 2: Event Layer (Mapeamento)

#### **EventMappingService**

**Responsabilidade:** Converter entidades JPA em DTOs Kafka

```

@Service
public class EventMappingService {

    /**
     * Mapeia Employee (entidade JPA) → EmployeeEventDTO (Kafka DTO)
     */
    public EmployeeEventDTO toDTO(Employee employee, EventType eventType) {
        return EmployeeEventDTO.builder()
            // Metadata do evento
            .eventId(UUID.randomUUID().toString())
            .eventType(eventType.name())
            .eventTimestamp(LocalDateTime.now())
            .correlationId(generateCorrelationId())
            
            // Dados do colaborador
            .sourceId(employee.getEmployeeId())
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
            .build();
    }
    
    /**
     * Gera Correlation ID para rastreabilidade
     * Formato: {timestamp}-{nodeId}-{sequence}
     */
    private String generateCorrelationId() {
        return String.format("%d-%s-%d",
            System.currentTimeMillis(),
            getNodeId(),
            correlationSequence.incrementAndGet()
        );
    }
    }

```

---

### Componente 3: Kafka Layer (Publicação)

#### **KafkaProducerService**

**Responsabilidade:** Publicar eventos no Kafka com garantias de entrega

```

@Service
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    
    /**
     * Publica evento com roteamento por tipo
     */
    public void publish(EmployeeEventDTO event) {
        // 1. Selecionar tópico baseado no tipo de evento
        String topic = selectTopic(event.getEventType());
        
        // 2. Definir partition key (garante ordem por employee)
        String partitionKey = event.getSourceId();
        
        // 3. Publicar com callback
        ListenableFuture<SendResult<String, EmployeeEventDTO>> future = 
            kafkaTemplate.send(topic, partitionKey, event);
        
        // 4. Callback assíncrono
        future.addCallback(
            result -> handleSuccess(event, result),
            ex -> handleFailure(event, ex)
        );
    }
    
    /**
     * Roteamento de tópicos
     */
    private String selectTopic(String eventType) {
        return switch(eventType) {
            case "CREATE" -> "employee-create";
            case "UPDATE" -> "employee-update";
            case "DELETE" -> "employee-delete";
            default -> throw new IllegalArgumentException(
                "Tipo de evento inválido: " + eventType);
        };
    }
    
    /**
     * Sucesso: registrar métrica
     */
    private void handleSuccess(EmployeeEventDTO event, 
                               SendResult<String, EmployeeEventDTO> result) {
        log.info("Evento publicado com sucesso: {} no tópico {} (partition={}, offset={})",
            event.getEventId(),
            result.getRecordMetadata().topic(),
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset()
        );
        
        meterRegistry.counter("events.published.success",
            "topic", result.getRecordMetadata().topic()
        ).increment();
    }
    
    /**
     * Falha: retry ou DLQ
     */
    private void handleFailure(EmployeeEventDTO event, Throwable ex) {
        log.error("Erro ao publicar evento {}: {}", 
                 event.getEventId(), ex.getMessage());
        
        meterRegistry.counter("events.published.failure",
            "error", ex.getClass().getSimpleName()
        ).increment();
        
        // Kafka já faz retry interno (retries=3)
        // Se falhar após retries, lança exceção
    }
    }

```

**Configuração Kafka Producer:**
```

spring:
kafka:
bootstrap-servers: localhost:9092,localhost:9093,localhost:9094
producer:
\# Garantias de entrega
acks: all                    \# RF=3
retries: 3
enable-idempotence: true     \# Exatamente uma vez

      # Performance
      compression-type: snappy
      batch-size: 16384
      linger-ms: 10
      
      # Serialização
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    ```

---

### Componente 4: Monitoring Layer

#### **MetricsCollector**

**Responsabilidade:** Coletar métricas customizadas para Prometheus

```

@Component
public class MetricsCollector {

    private final MeterRegistry registry;
    
    // Contadores
    private final Counter eventsPublished;
    private final Counter cdcErrors;
    
    // Gauges
    private final AtomicInteger lastBatchSize = new AtomicInteger(0);
    
    // Timers (histograms)
    private final Timer cdcDuration;
    
    public MetricsCollector(MeterRegistry registry) {
        this.registry = registry;
        
        // Registrar métricas
        this.eventsPublished = Counter.builder("events_published_total")
            .description("Total de eventos publicados no Kafka")
            .tag("service", "producer")
            .register(registry);
        
        this.cdcDuration = Timer.builder("cdc_polling_duration_seconds")
            .description("Tempo de execução do polling CDC")
            .tag("service", "producer")
            .register(registry);
        
        // Gauge dinâmico
        Gauge.builder("cdc_last_batch_size", lastBatchSize, AtomicInteger::get)
            .description("Tamanho do último batch processado")
            .register(registry);
    }
    
    public void recordEventPublished(String topic) {
        eventsPublished.increment();
    }
    
    public void recordCdcDuration(long durationMs) {
        cdcDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordBatchSize(int size) {
        lastBatchSize.set(size);
    }
    }

```

---

## Consumer Service - Componentes

### Visão Geral da Estrutura

```

consumer-service/
├── src/main/java/com/esocial/consumer/
│   ├── config/
│   │   ├── KafkaConsumerConfig.java
│   │   └── ValidationConfig.java
│   ├── service/
│   │   ├── KafkaConsumerService.java         (Kafka Layer)
│   │   ├── ValidationEngine.java             (Validation Layer)
│   │   ├── PersistenceService.java           (Persistence Layer)
│   │   └── DLQService.java                   (DLQ Layer)
│   ├── validation/
│   │   ├── ValidationRule.java               (Interface)
│   │   ├── structural/                       (6 regras)
│   │   └── business/                         (5 regras)
│   ├── controller/
│   │   ├── ValidationReportController.java   (API Layer)
│   │   └── DLQManagementController.java
│   ├── repository/
│   │   ├── EmployeeRepository.java
│   │   ├── EmployeeHistoryRepository.java
│   │   └── DLQEventRepository.java
│   └── model/
│       ├── entity/
│       └── dto/

```

---

### Componente 1: Kafka Layer (Consumo)

#### **KafkaConsumerService**

**Responsabilidade:** Consumir eventos de 3 tópicos com ACK manual

```

@Service
@Slf4j
public class KafkaConsumerService {

    private final ValidationEngine validationEngine;
    private final PersistenceService persistenceService;
    private final DLQService dlqService;
    
    /**
     * Consome eventos de 3 tópicos simultaneamente
     */
    @KafkaListener(
        topics = {
            "employee-create",
            "employee-update",
            "employee-delete"
        },
        groupId = "esocial-consumer-group",
        concurrency = "3"  // 3 threads (1 por tópico)
    )
    public void consume(
        @Payload EmployeeEventDTO event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.OFFSET) Long offset,
        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
        Acknowledgment ack
    ) {
        MDC.put("correlationId", event.getCorrelationId());
        MDC.put("eventId", event.getEventId());
        
        log.info("Consumindo evento {} do tópico {} (partition={}, offset={})",
            event.getEventId(), topic, partition, offset);
        
        try {
            // 1. Validar evento (3 camadas)
            ValidationResult result = validationEngine.validate(event);
            
            if (result.isValid()) {
                // 2. Persistir se válido
                persistenceService.persist(event, offset, partition);
                
                log.info("Evento {} processado com sucesso", event.getEventId());
                
                // 3. Commit manual (garante at-least-once)
                ack.acknowledge();
                
            } else {
                // 4. Enviar para DLQ se inválido
                log.warn("Evento {} falhou na validação: {}", 
                        event.getEventId(), result.getErrors());
                
                dlqService.sendToDLQ(event, result.getErrors(), offset);
                
                // 5. Commit mesmo com falha (não bloqueia fila)
                ack.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("Erro inesperado ao processar evento {}", 
                     event.getEventId(), e);
            
            // 6. DLQ para erros não capturados
            dlqService.sendToDLQ(event, e.getMessage(), offset);
            ack.acknowledge();
            
        } finally {
            MDC.clear();
        }
    }
    }

```

**Configuração Kafka Consumer:**
```

spring:
kafka:
bootstrap-servers: localhost:9092,localhost:9093,localhost:9094
consumer:
group-id: esocial-consumer-group
auto-offset-reset: earliest
enable-auto-commit: false     \# ACK manual
max-poll-records: 100

      # Deserialização
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.esocial.consumer.dto
    ```

---

### Componente 2: Validation Layer (Motor de Validações)

#### **ValidationEngine**

**Responsabilidade:** Executar regras de validação em 3 camadas

```

@Service
@Slf4j
public class ValidationEngine {

    private final List<ValidationRule> structuralRules;
    private final List<ValidationRule> businessRules;
    private final List<ValidationRule> esocialRules;  // Futuro
    
    public ValidationEngine(
        @Qualifier("structuralRules") List<ValidationRule> structuralRules,
        @Qualifier("businessRules") List<ValidationRule> businessRules
    ) {
        this.structuralRules = structuralRules;
        this.businessRules = businessRules;
        this.esocialRules = List.of();  // Placeholder
    }
    
    /**
     * Valida evento em 3 camadas (fail-fast)
     */
    public ValidationResult validate(EmployeeEventDTO event) {
        ValidationResult result = new ValidationResult();
        
        log.debug("Iniciando validação de {}", event.getEventId());
        
        // Camada 1: Validações Estruturais (6 regras)
        log.debug("Executando validações estruturais...");
        executeRules(structuralRules, event, result);
        
        if (result.hasError()) {
            log.warn("Validação estrutural falhou para {}", event.getEventId());
            return result;  // Short-circuit
        }
        
        // Camada 2: Validações de Negócio (5 regras)
        log.debug("Executando validações de negócio...");
        executeRules(businessRules, event, result);
        
        if (result.hasError()) {
            log.warn("Validação de negócio falhou para {}", event.getEventId());
            return result;  // Short-circuit
        }
        
        // Camada 3: Validações eSocial (futuro)
        // executeRules(esocialRules, event, result);
        
        log.info("Validação completa: {} com {} warnings",
            event.getEventId(), result.getWarnings().size());
        
        return result;
    }
    
    /**
     * Executa lista de regras
     */
    private void executeRules(List<ValidationRule> rules, 
                              EmployeeEventDTO event, 
                              ValidationResult result) {
        for (ValidationRule rule : rules) {
            try {
                rule.validate(event, result);
                
                // Para em primeiro ERROR (não em WARNING)
                if (result.hasError()) {
                    break;
                }
            } catch (Exception e) {
                log.error("Erro ao executar regra {}: {}", 
                         rule.getRuleName(), e.getMessage());
                result.addError(
                    "VALIDATION_ENGINE_ERROR",
                    "system",
                    "Erro interno: " + e.getMessage(),
                    ValidationSeverity.ERROR
                );
                break;
            }
        }
    }
    }

```

---

#### **Exemplo de Regra Estrutural**

```

@Component
@Order(1)
public class CpfFormatValidationRule implements ValidationRule {

    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    
    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        String cpf = event.getCpf();
        
        if (cpf == null || !CPF_PATTERN.matcher(cpf).matches()) {
            result.addError(
                "INVALID_CPF_FORMAT",
                "cpf",
                String.format("CPF '%s' deve ter 11 dígitos numéricos", cpf),
                ValidationSeverity.ERROR
            );
        }
    }
    
    @Override
    public String getRuleName() {
        return "CpfFormatValidationRule";
    }
    
    @Override
    public ValidationSeverity getSeverity() {
        return ValidationSeverity.ERROR;
    }
    }

```

---

### Componente 3: Persistence Layer

#### **PersistenceService**

**Responsabilidade:** Persistir dados com versionamento e audit trail

```

@Service
@Transactional
@Slf4j
public class PersistenceService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeHistoryRepository historyRepository;
    
    /**
     * Persiste evento com versionamento otimista
     */
    public void persist(EmployeeEventDTO event, Long kafkaOffset, Integer partition) {
        log.info("Persistindo evento {} (offset={})", event.getEventId(), kafkaOffset);
        
        Employee employee = switch(event.getEventType()) {
            case "CREATE" -> createEmployee(event);
            case "UPDATE" -> updateEmployee(event);
            case "DELETE" -> softDeleteEmployee(event);
            default -> throw new IllegalArgumentException(
                "Tipo inválido: " + event.getEventType());
        };
        
        // Metadata Kafka
        employee.setKafkaOffset(kafkaOffset);
        employee.setKafkaPartition(partition);
        employee.setCorrelationId(UUID.fromString(event.getCorrelationId()));
        
        // Salvar employee
        employeeRepository.save(employee);
        
        // Criar histórico
        EmployeeHistory history = createHistory(employee, event);
        historyRepository.save(history);
        
        log.info("Evento {} persistido com sucesso (version={})",
            event.getEventId(), employee.getVersion());
    }
    
    private Employee createEmployee(EmployeeEventDTO event) {
        return Employee.builder()
            .sourceId(event.getSourceId())
            .cpf(event.getCpf())
            .pis(event.getPis())
            .fullName(event.getFullName())
            .birthDate(event.getBirthDate())
            .admissionDate(event.getAdmissionDate())
            .jobTitle(event.getJobTitle())
            .department(event.getDepartment())
            .salary(event.getSalary())
            .status(event.getStatus())
            .esocialStatus("PENDING")
            .version(1)  // Versão inicial
            .build();
    }
    
    private Employee updateEmployee(EmployeeEventDTO event) {
        Employee existing = employeeRepository
            .findBySourceId(event.getSourceId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Employee não encontrado: " + event.getSourceId()));
        
        // Atualizar campos
        existing.setSalary(event.getSalary());
        existing.setJobTitle(event.getJobTitle());
        existing.setStatus(event.getStatus());
        existing.setVersion(existing.getVersion() + 1);  // Incrementar versão
        existing.setEsocialStatus("PENDING");  // Requer envio ao eSocial
        
        return existing;
    }
    
    private Employee softDeleteEmployee(EmployeeEventDTO event) {
        Employee existing = employeeRepository
            .findBySourceId(event.getSourceId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Employee não encontrado: " + event.getSourceId()));
        
        // Soft delete
        existing.setStatus("TERMINATED");
        existing.setTerminationDate(event.getTerminationDate());
        existing.setVersion(existing.getVersion() + 1);
        existing.setEsocialStatus("PENDING");
        
        return existing;
    }
    }

```

---

## Diagrama PlantUML Completo

### Producer Service - Componentes

```

@startuml Producer Service - Componentes
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

Container_Boundary(producer, "Producer Service") {

    Component(scheduler, "CdcPollingScheduler", "Spring @Scheduled", "Dispara polling a cada 5s")
    Component(cdc, "ChangeDataCaptureService", "Java", "Captura mudanças via SQL")
    Component(offset, "OffsetManager", "Java", "Gerencia último timestamp")
    Component(mapper, "EventMappingService", "Java", "Entity → DTO")
    Component(enricher, "EventEnricher", "Java", "Adiciona Correlation ID")
    Component(producer_svc, "KafkaProducerService", "Spring Kafka", "Publica eventos")
    Component(metrics, "MetricsCollector", "Micrometer", "Coleta métricas Prometheus")
    Component(health, "HealthIndicator", "Spring Actuator", "Health checks")
    }

ContainerDb(oracle, "PostgreSQL Origem", "PostgreSQL 15", "source schema")
Container(kafka, "Apache Kafka", "Confluent 7.5", "Message Broker")
Container(prometheus, "Prometheus", "2.45", "Métricas")

Rel(scheduler, cdc, "Trigger", "@Scheduled")
Rel(cdc, oracle, "SELECT ... WHERE updated_at > ?", "JDBC")
Rel(cdc, offset, "Get/Update offset")
Rel(cdc, mapper, "Employee entity")
Rel(mapper, enricher, "DTO")
Rel(enricher, producer_svc, "Event + Correlation ID")
Rel(producer_svc, kafka, "send(topic, key, value)", "Kafka Protocol")
Rel(producer_svc, metrics, "Record metrics")
Rel(metrics, prometheus, "Expose /actuator/prometheus", "HTTP")

@enduml

```

### Consumer Service - Componentes

```

@startuml Consumer Service - Componentes
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

Container_Boundary(consumer, "Consumer Service") {

    Component(consumer_svc, "KafkaConsumerService", "Spring Kafka", "Consome 3 tópicos")
    Component(engine, "ValidationEngine", "Java", "Motor de validações (3 camadas)")
    
    Component_Ext(struct_rules, "Structural Rules", "Java", "6 regras formato")
    Component_Ext(business_rules, "Business Rules", "Java", "5 regras negócio")
    
    Component(persist, "PersistenceService", "Spring Data JPA", "Persistência transacional")
    Component(dlq_svc, "DLQService", "Java", "Dead Letter Queue")
    
    Component(api, "ValidationReportController", "Spring Web", "GET /api/v1/validation/...")
    Component(dlq_api, "DLQManagementController", "Spring Web", "POST /api/v1/dlq/{id}/retry")
    }

Container(kafka, "Apache Kafka", "Confluent 7.5")
ContainerDb(postgres, "PostgreSQL Destino", "PostgreSQL 15")

Rel(kafka, consumer_svc, "poll()", "Kafka Protocol")
Rel(consumer_svc, engine, "validate(event)")
Rel(engine, struct_rules, "Execute")
Rel(engine, business_rules, "Execute if structural OK")
Rel(engine, consumer_svc, "ValidationResult")

Rel(consumer_svc, persist, "persist() if valid")
Rel(consumer_svc, dlq_svc, "sendToDLQ() if invalid")

Rel(persist, postgres, "INSERT/UPDATE", "JDBC")
Rel(dlq_svc, postgres, "INSERT into dlq_events", "JDBC")
Rel(api, postgres, "SELECT errors", "JDBC")
Rel(dlq_api, postgres, "SELECT/UPDATE dlq", "JDBC")

@enduml

```

---

## Decisões de Design

### 1. Por Que CDC via Polling?

**Alternativas consideradas:**
- ✅ **Polling** (escolhido): Simples, independente de DB
- ❌ **Triggers DB:** Acoplamento com DB, complexo
- ❌ **Debezium:** Overhead para MVP, migração futura

**Referência:** ADR-0002

### 2. Por Que Validações em Componentes Separados?

**Padrão Strategy Pattern:**
- ✅ Extensível: adicionar regras sem modificar engine
- ✅ Testável: cada regra isoladamente
- ✅ Maintainável: regras independentes

**Referência:** ADR-0007

### 3. Por Que ACK Manual no Consumer?

**Garantia At-Least-Once:**
```

// ACK somente após processar (persist OU dlq)
ack.acknowledge();

```

**Benefícios:**
- ✅ Zero perda de dados
- ✅ Controle fino sobre offset
- ❌ Possível duplicação (mitigada por idempotência)

---

## Métricas por Componente

| Componente | Métrica | Tipo | Descrição |
|------------|---------|------|-----------|
| **CDC** | `cdc_polling_duration_seconds` | Histogram | Latência CDC P50/P95/P99 |
| **CDC** | `cdc_records_processed_total` | Counter | Total processado |
| **Producer** | `events_published_total` | Counter | Publicações sucesso |
| **Producer** | `kafka_publish_errors_total` | Counter | Erros publicação |
| **Consumer** | `events_consumed_total` | Counter | Eventos consumidos |
| **Validation** | `validation_success_total` | Counter | Validações OK |
| **Validation** | `validation_failure_total` | Counter | Validações falha |
| **Validation** | `validation_duration_seconds` | Histogram | Tempo validação |
| **DLQ** | `dlq_events_pending` | Gauge | Eventos na DLQ |
| **Persistence** | `persistence_duration_seconds` | Histogram | Tempo persistência |

---

## Próximos Passos

1. **Level 4 - Código:** Diagrama de classes detalhado
2. **Deployment:** Diagrama de infraestrutura
3. **Sequência:** Diagrama de fluxo E2E

---

## Referências

- [C4 Model Documentation](https://c4model.com/)
- [PlantUML C4 Library](https://github.com/plantuml-stdlib/C4-PlantUML)
- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/)
- ADR-0002: CDC via Polling
- ADR-0007: Three-Layer Validation

---

**Última atualização:** 2025-11-22  
**Autor:** Márcio Kuroki Gonçalves