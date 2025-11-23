# C4 Model - Level 4: Código

**Versão:** 1.0  
**Data:** 2025-11-22  
**Autor:** Márcio Kuroki Gonçalves  
**Projeto:** Pipeline ETL eSocial

---

## Índice

1. [Visão Geral](#visão-geral)
2. [Diagrama de Classes - Motor de Validações](#diagrama-de-classes---motor-de-validações)
3. [Diagrama de Classes - Entidades JPA](#diagrama-de-classes---entidades-jpa)
4. [Diagrama de Classes - DTOs e Eventos](#diagrama-de-classes---dtos-e-eventos)
5. [Diagrama de Sequência - Fluxo E2E Completo](#diagrama-de-sequência---fluxo-e2e-completo)
6. [Diagrama de Sequência - Validação em Camadas](#diagrama-de-sequência---validação-em-camadas)
7. [Diagrama de Sequência - Reprocessamento DLQ](#diagrama-de-sequência---reprocessamento-dlq)
8. [Padrões de Design Implementados](#padrões-de-design-implementados)

---

## Visão Geral

Este documento representa o **nível mais detalhado** da arquitetura C4, mostrando classes, interfaces, métodos e fluxos de execução do código-fonte.

### Escopo

- ✅ Classes principais de cada camada
- ✅ Interfaces e abstrações
- ✅ Relacionamentos (herança, composição, associação)
- ✅ Fluxos de execução (diagramas de sequência)
- ✅ Padrões de design aplicados

---

## Diagrama de Classes - Motor de Validações

### Hierarquia de Validações

```

@startuml Validation Engine - Classes
!theme plain
skinparam linetype ortho

' Interfaces
interface ValidationRule {
+validate(event: EmployeeEventDTO, result: ValidationResult): void
+getRuleName(): String
+getSeverity(): ValidationSeverity
+getOrder(): int
}

' Classe abstrata
abstract class AbstractValidationRule {
\#ruleName: String
\#severity: ValidationSeverity
\#order: int

    +getRuleName(): String
    +getSeverity(): ValidationSeverity
    +getOrder(): int
    #{abstract} doValidate(event: EmployeeEventDTO, result: ValidationResult): void
    +validate(event: EmployeeEventDTO, result: ValidationResult): void
    }

' Regras Estruturais
package "structural" {
class CpfFormatValidationRule {
-CPF_PATTERN: Pattern
+doValidate(): void
}

    class PisFormatValidationRule {
        -PIS_PATTERN: Pattern
        +doValidate(): void
    }
    
    class RequiredFieldsValidationRule {
        -REQUIRED_FIELDS: Set<String>
        +doValidate(): void
    }
    
    class NumericFieldsValidationRule {
        +doValidate(): void
    }
    
    class DateFormatValidationRule {
        +doValidate(): void
    }
    
    class EnumValidationRule {
        -VALID_STATUS: Set<String>
        +doValidate(): void
    }
    }

' Regras de Negócio
package "business" {
class MinimumAgeValidationRule {
-MINIMUM_AGE: int = 16
+doValidate(): void
}

    class FutureDateValidationRule {
        +doValidate(): void
    }
    
    class LogicalDateOrderValidationRule {
        +doValidate(): void
    }
    
    class MinimumSalaryValidationRule {
        -MINIMUM_SALARY: BigDecimal
        +doValidate(): void
    }
    
    class StatusTransitionValidationRule {
        -VALID_TRANSITIONS: Map<String, Set<String>>
        +doValidate(): void
    }
    }

' Engine
class ValidationEngine {
-structuralRules: List<ValidationRule>
-businessRules: List<ValidationRule>
-metricsCollector: MetricsCollector

    +validate(event: EmployeeEventDTO): ValidationResult
    -executeRules(rules: List<ValidationRule>, event: EmployeeEventDTO, result: ValidationResult): void
    ```
    -sortRulesByOrder(rules: List<ValidationRule>): List<ValidationRule>
    ```
    }

' Result
class ValidationResult {
-valid: boolean
-errors: List<ValidationError>
-warnings: List<ValidationError>

    +isValid(): boolean
    +hasError(): boolean
    +hasWarning(): boolean
    +addError(ruleName, fieldName, message, severity): void
    +getErrors(): List<ValidationError>
    +getWarnings(): List<ValidationError>
    }

' Error
class ValidationError {
-ruleName: String
-fieldName: String
-errorMessage: String
-severity: ValidationSeverity
-timestamp: LocalDateTime

    +getRuleName(): String
    +getFieldName(): String
    +getMessage(): String
    +getSeverity(): ValidationSeverity
    }

' Enum
enum ValidationSeverity {
ERROR
WARNING
INFO

    +isBlocker(): boolean
    }

' Relacionamentos
ValidationRule <|.. AbstractValidationRule
AbstractValidationRule <|-- CpfFormatValidationRule
AbstractValidationRule <|-- PisFormatValidationRule
AbstractValidationRule <|-- RequiredFieldsValidationRule
AbstractValidationRule <|-- NumericFieldsValidationRule
AbstractValidationRule <|-- DateFormatValidationRule
AbstractValidationRule <|-- EnumValidationRule

AbstractValidationRule <|-- MinimumAgeValidationRule
AbstractValidationRule <|-- FutureDateValidationRule
AbstractValidationRule <|-- LogicalDateOrderValidationRule
AbstractValidationRule <|-- MinimumSalaryValidationRule
AbstractValidationRule <|-- StatusTransitionValidationRule

ValidationEngine o-- ValidationRule : "uses *"
ValidationEngine ..> ValidationResult : "creates"
ValidationResult o-- ValidationError : "contains *"
ValidationError --> ValidationSeverity

note right of ValidationEngine
Padrões aplicados:
- Strategy Pattern
- Chain of Responsibility
- Template Method
end note

@enduml

```

### Código das Classes Principais

#### Interface ValidationRule

```

/**

* Contrato para regras de validação
* Padrão: Strategy Pattern
*/
public interface ValidationRule {

/**
    * Executa validação e adiciona erros ao resultado
*/
void validate(EmployeeEventDTO event, ValidationResult result);

/**
    * Nome identificador da regra (para logs e métricas)
*/
String getRuleName();

/**
    * Severidade da regra (ERROR bloqueia persistência)
*/
ValidationSeverity getSeverity();

/**
    * Ordem de execução (menor = primeiro)
*/
default int getOrder() {
return 100;
}
}

```

#### Classe Abstrata AbstractValidationRule

```

/**

* Classe base para regras de validação
* Padrão: Template Method
*/
public abstract class AbstractValidationRule implements ValidationRule {

protected final String ruleName;
protected final ValidationSeverity severity;
protected final int order;

protected AbstractValidationRule(String ruleName,
ValidationSeverity severity,
int order) {
this.ruleName = ruleName;
this.severity = severity;
this.order = order;
}

@Override
public final void validate(EmployeeEventDTO event, ValidationResult result) {
try {
// Template method: delega para subclasse
doValidate(event, result);
} catch (Exception e) {
// Captura exceções e adiciona como erro
result.addError(
ruleName,
"system",
"Erro interno na validação: " + e.getMessage(),
ValidationSeverity.ERROR
);
}
}

/**
    * Implementado pelas subclasses com lógica específica
*/
protected abstract void doValidate(EmployeeEventDTO event,
ValidationResult result);

@Override
public String getRuleName() {
return ruleName;
}

@Override
public ValidationSeverity getSeverity() {
return severity;
}

@Override
public int getOrder() {
return order;
}
}

```

#### Exemplo: CpfFormatValidationRule

```

@Component
@Order(1)  // Primeira regra estrutural
public class CpfFormatValidationRule extends AbstractValidationRule {

    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    
    public CpfFormatValidationRule() {
        super("INVALID_CPF_FORMAT", ValidationSeverity.ERROR, 1);
    }
    
    @Override
    protected void doValidate(EmployeeEventDTO event, ValidationResult result) {
        String cpf = event.getCpf();
        
        if (cpf == null) {
            result.addError(
                getRuleName(),
                "cpf",
                "CPF é obrigatório",
                getSeverity()
            );
            return;
        }
        
        if (!CPF_PATTERN.matcher(cpf).matches()) {
            result.addError(
                getRuleName(),
                "cpf",
                String.format("CPF '%s' deve ter exatamente 11 dígitos numéricos", cpf),
                getSeverity()
            );
        }
        
        // Validação de dígitos verificadores (opcional)
        if (!isValidCpfChecksum(cpf)) {
            result.addError(
                getRuleName(),
                "cpf",
                String.format("CPF '%s' possui dígitos verificadores inválidos", cpf),
                getSeverity()
            );
        }
    }
    
    private boolean isValidCpfChecksum(String cpf) {
        // Implementação algoritmo de validação CPF
        // ...
        return true;  // Simplificado
    }
    }

```

---

## Diagrama de Classes - Entidades JPA

### Modelo de Domínio

```

@startuml Domain Model - Entities
!theme plain

' Entidades principais
class Employee {
-id: Long <<PK>>
-sourceId: String <<UK>>
-cpf: String <<UK>>
-pis: String
-fullName: String
-birthDate: LocalDate
-admissionDate: LocalDate
-terminationDate: LocalDate
-jobTitle: String
-department: String
-salary: BigDecimal
-status: String
-esocialStatus: String
-version: Integer
-kafkaOffset: Long <<UK>>
-kafkaPartition: Integer
-correlationId: UUID
-createdAt: LocalDateTime
-updatedAt: LocalDateTime

    +incrementVersion(): void
    +isTerminated(): boolean
    }

class EmployeeHistory {
-historyId: Long <<PK>>
-employeeId: Long <<FK>>
-sourceId: String
-operation: String
-version: Integer
-changedAt: LocalDateTime
-changedBy: String
-cpf: String
-fullName: String
-salary: BigDecimal
-jobTitle: String
-status: String
-kafkaOffset: Long
-correlationId: UUID
}

class ValidationError {
-id: Long <<PK>>
-eventId: String
-sourceId: String
-validationRule: String
-errorMessage: String
-severity: String
-fieldName: String
-fieldValue: String
-eventPayload: String <<JSONB>>
-kafkaOffset: Long
-correlationId: UUID
-createdAt: LocalDateTime
}

class DLQEvent {
-id: Long <<PK>>
-eventId: String <<UK>>
-eventType: String
-eventPayload: String <<JSONB>>
-errorMessage: String
-stackTrace: String
-retryCount: Integer
-maxRetries: Integer
-status: String
-kafkaOffset: Long
-lastRetryAt: LocalDateTime
-createdAt: LocalDateTime
-updatedAt: LocalDateTime

    +canRetry(): boolean
    +incrementRetryCount(): void
    +markAsReprocessed(): void
    +markAsFailed(): void
    }

' Enums
enum EmployeeStatus {
ACTIVE
INACTIVE
TERMINATED
}

enum ESocialStatus {
PENDING
SENT
CONFIRMED
REJECTED
}

enum DLQStatus {
PENDING
REPROCESSING
REPROCESSED
FAILED
}

' Relacionamentos
Employee "1" -- "0..*" EmployeeHistory : histórico
Employee "1" -- "0..*" ValidationError : erros
Employee "1" -- "0..*" DLQEvent : dlq

Employee --> EmployeeStatus
Employee --> ESocialStatus
DLQEvent --> DLQStatus

note top of Employee
Versionamento otimista:
@Version Integer version

    Audit trail via trigger:
    INSERT INTO audit.employees_history
    end note

note right of DLQEvent
JSONB para payload flexível:
@Type(type = "jsonb")

    Retry policy:
    maxRetries = 3
    end note

@enduml

```

### Código das Entidades

#### Employee (Principal)

```

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
    
    @Column(name = "source_id", unique = true, nullable = false, length = 20)
    private String sourceId;
    
    @Column(name = "cpf", unique = true, nullable = false, length = 11)
    private String cpf;
    
    @Column(name = "pis", length = 11)
    private String pis;
    
    @Column(name = "full_name", nullable = false, length = 200)
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
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;
    
    @Column(name = "esocial_status", length = 20)
    @Enumerated(EnumType.STRING)
    private ESocialStatus esocialStatus;
    
    /**
     * Versionamento otimista (evita conflitos de atualização)
     */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
    
    /**
     * Offset Kafka (idempotência)
     */
    @Column(name = "kafka_offset", unique = true)
    private Long kafkaOffset;
    
    @Column(name = "kafka_partition")
    private Integer kafkaPartition;
    
    @Column(name = "correlation_id")
    private UUID correlationId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * Incrementa versão manualmente (além do @Version automático)
     */
    public void incrementVersion() {
        this.version = (this.version == null) ? 1 : this.version + 1;
    }
    
    /**
     * Verifica se colaborador está desligado
     */
    public boolean isTerminated() {
        return EmployeeStatus.TERMINATED.equals(this.status);
    }
    }

```

#### DLQEvent (Dead Letter Queue)

```

@Entity
@Table(name = "dlq_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DLQEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", unique = true, nullable = false)
    private String eventId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    /**
     * JSONB para armazenar payload completo
     */
    @Type(type = "jsonb")
    @Column(name = "event_payload", columnDefinition = "jsonb", nullable = false)
    private String eventPayload;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;
    
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;
    
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DLQStatus status = DLQStatus.PENDING;
    
    @Column(name = "kafka_offset")
    private Long kafkaOffset;
    
    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * Verifica se evento pode ser retentado
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetries 
                && DLQStatus.PENDING.equals(this.status);
    }
    
    /**
     * Incrementa contador de retry
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
        
        if (this.retryCount >= this.maxRetries) {
            this.status = DLQStatus.FAILED;
        }
    }
    
    /**
     * Marca como reprocessado com sucesso
     */
    public void markAsReprocessed() {
        this.status = DLQStatus.REPROCESSED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Marca como falha permanente
     */
    public void markAsFailed() {
        this.status = DLQStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }
    }

```

---

## Diagrama de Classes - DTOs e Eventos

```

@startuml DTOs and Events
!theme plain

' DTOs
class EmployeeEventDTO {
-eventId: String
-eventType: String
-eventTimestamp: LocalDateTime
-correlationId: String
-sourceId: String
-cpf: String
-pis: String
-fullName: String
-birthDate: LocalDate
-admissionDate: LocalDate
-terminationDate: LocalDate
-jobTitle: String
-department: String
-salary: BigDecimal
-status: String

    +toJson(): String
    +fromJson(json: String): EmployeeEventDTO
    }

class ValidationResultDTO {
-eventId: String
-valid: boolean
-errors: List<ValidationErrorDTO>
-warnings: List<ValidationErrorDTO>
-processedAt: LocalDateTime
}

class ValidationErrorDTO {
-ruleName: String
-fieldName: String
-errorMessage: String
-severity: String
}

class DLQEventDTO {
-id: Long
-eventId: String
-eventType: String
-errorMessage: String
-retryCount: Integer
-maxRetries: Integer
-status: String
-canRetry: boolean
-createdAt: LocalDateTime
}

class DashboardDTO {
-statistics: StatisticsDTO
-recentErrors: List<ValidationErrorDTO>
-dlqStatistics: DLQStatisticsDTO
-generatedAt: LocalDateTime
}

class StatisticsDTO {
-totalEvents: Long
-successfulEvents: Long
-failedEvents: Long
-successRate: Double
-averageProcessingTime: Duration
}

class DLQStatisticsDTO {
-totalEvents: Long
-pendingEvents: Long
-reprocessedEvents: Long
-failedEvents: Long
}

' Relacionamentos
ValidationResultDTO o-- ValidationErrorDTO : "contains *"
DashboardDTO o-- StatisticsDTO
DashboardDTO o-- DLQStatisticsDTO
DashboardDTO o-- ValidationErrorDTO : "recent *"

note right of EmployeeEventDTO
Serializado como JSON
para Kafka e DLQ

    @JsonProperty para
    snake_case mapping
    end note

@enduml

```

---

## Diagrama de Sequência - Fluxo E2E Completo

```

@startuml Fluxo End-to-End Completo
!theme plain
autonumber

actor Usuario
participant "PostgreSQL\nOrigem" as Oracle
participant "CDC\nService" as CDC
participant "Event\nMapper" as Mapper
participant "Kafka\nProducer" as KProducer
participant "Kafka\nBroker" as Kafka
participant "Kafka\nConsumer" as KConsumer
participant "Validation\nEngine" as Validation
participant "Persistence\nService" as Persistence
participant "PostgreSQL\nDestino" as Postgres
participant "Audit\nHistory" as Audit

Usuario -> Oracle: INSERT INTO employees
activate Oracle
Oracle --> Usuario: OK
deactivate Oracle

... Aguardar 5 segundos (CDC polling) ...

CDC -> Oracle: SELECT * WHERE updated_at > :lastOffset
activate CDC
activate Oracle
Oracle --> CDC: List<Employee> (1 registro)
deactivate Oracle

CDC -> CDC: determineEventType(employee)\n→ CREATE

CDC -> Mapper: toDTO(employee, CREATE)
activate Mapper
Mapper -> Mapper: generateCorrelationId()
Mapper --> CDC: EmployeeEventDTO
deactivate Mapper

CDC -> KProducer: publish(event)
activate KProducer

KProducer -> KProducer: selectTopic(CREATE)\n→ "employee-create"
KProducer -> KProducer: partitionKey = sourceId

KProducer -> Kafka: send(topic, key, value)
activate Kafka
Kafka -> Kafka: Replicate (RF=3)
Kafka --> KProducer: SendResult\n(partition=0, offset=123)
deactivate KProducer

note right of Kafka
Garantias:
- acks=all (RF=3)
- idempotence=true
end note

KConsumer -> Kafka: poll()
activate KConsumer
Kafka --> KConsumer: ConsumerRecord\n(offset=123)
deactivate Kafka

KConsumer -> Validation: validate(event)
activate Validation

' Camada 1: Estrutural
Validation -> Validation: executeRules(structuralRules)
Validation -> Validation: CpfFormatValidationRule ✓
Validation -> Validation: RequiredFieldsValidationRule ✓
Validation -> Validation: DateFormatValidationRule ✓

' Camada 2: Negócio
Validation -> Validation: executeRules(businessRules)
Validation -> Validation: MinimumAgeValidationRule ✓
Validation -> Validation: FutureDateValidationRule ✓
Validation -> Validation: MinimumSalaryValidationRule ⚠️ WARNING

Validation --> KConsumer: ValidationResult\n(valid=true, warnings=[SALARY])
deactivate Validation

alt Validação Sucesso
KConsumer -> Persistence: persist(event, offset=123)
activate Persistence

    Persistence -> Persistence: mapToEntity(event)\n→ Employee
    Persistence -> Persistence: employee.setVersion(1)
    Persistence -> Persistence: employee.setKafkaOffset(123)
    
    Persistence -> Postgres: BEGIN TRANSACTION
    activate Postgres
    
    Persistence -> Postgres: INSERT INTO employees
    Postgres --> Persistence: id=1, version=1
    
    Persistence -> Audit: INSERT INTO employees_history
    activate Audit
    Audit --> Persistence: history_id=1
    deactivate Audit
    
    Persistence -> Postgres: COMMIT
    Postgres --> Persistence: OK
    deactivate Postgres
    
    Persistence --> KConsumer: Success
    deactivate Persistence
    
    KConsumer -> Kafka: ack.acknowledge()
    note right: Commit manual do offset
    else Validação Falha
KConsumer -> Postgres: INSERT INTO dlq_events
activate Postgres
Postgres --> KConsumer: dlq_id=1
deactivate Postgres

    KConsumer -> Postgres: INSERT INTO validation_errors
    activate Postgres
    Postgres --> KConsumer: OK
    deactivate Postgres
    
    KConsumer -> Kafka: ack.acknowledge()
    note right: Commit mesmo com falha\n(não bloqueia fila)
    end

deactivate KConsumer
deactivate CDC

Usuario -> Postgres: SELECT * FROM employees\nWHERE source_id = 'EMP100'
activate Postgres
Postgres --> Usuario: Employee (version=1)
deactivate Postgres

Usuario -> Audit: SELECT * FROM employees_history\nWHERE source_id = 'EMP100'
activate Audit
Audit --> Usuario: EmployeeHistory (operation=INSERT)
deactivate Audit

@enduml

```

---

## Diagrama de Sequência - Validação em Camadas

```

@startuml Validação em Três Camadas
!theme plain
autonumber

participant "Kafka\nConsumer" as Consumer
participant "Validation\nEngine" as Engine
participant "Structural\nRules" as Structural
participant "Business\nRules" as Business
participant "Validation\nResult" as Result

Consumer -> Engine: validate(event)
activate Engine

Engine -> Result: new ValidationResult()
activate Result

' ===== CAMADA 1: ESTRUTURAL =====
Engine -> Engine: executeRules(structuralRules)

loop for each structural rule
Engine -> Structural: validate(event, result)
activate Structural

    alt CPF Format Check
        Structural -> Structural: matches("\\d{11}")
        Structural -> Result: addError("INVALID_CPF_FORMAT")
    end
    
    alt PIS Format Check
        Structural -> Structural: matches("\\d{11}") if not null
    end
    
    alt Required Fields
        Structural -> Structural: checkNotNull(fullName)
        Structural -> Structural: checkNotNull(admissionDate)
    end
    
    Structural --> Engine: void
    deactivate Structural
    
    alt result.hasError()
        Engine --> Consumer: ValidationResult\n(valid=false)
        note right: Short-circuit!\nNão executa camada 2
    end
    end

' ===== CAMADA 2: NEGÓCIO =====
Engine -> Engine: executeRules(businessRules)

loop for each business rule
Engine -> Business: validate(event, result)
activate Business

    alt Minimum Age Check
        Business -> Business: age = Period.between(birthDate, admissionDate)
        Business -> Business: age >= 16 ?
        
        alt age < 16
            Business -> Result: addError("MINIMUM_AGE_VIOLATION")
        end
    end
    
    alt Future Date Check
        Business -> Business: birthDate <= today ?
        Business -> Business: admissionDate <= today ?
        
        alt date > today
            Business -> Result: addError("FUTURE_DATE")
        end
    end
    
    alt Minimum Salary Check
        Business -> Business: salary >= MINIMUM_SALARY ?
        
        alt salary < minimum
            Business -> Result: addWarning("BELOW_MINIMUM_SALARY")
            note right: WARNING não bloqueia
        end
    end
    
    Business --> Engine: void
    deactivate Business
    
    alt result.hasError()
        Engine --> Consumer: ValidationResult\n(valid=false)
        note right: Short-circuit!\nPara em primeiro ERROR
    end
    end

' ===== RESULTADO FINAL =====
Engine -> Result: isValid()
Result --> Engine: true/false

alt result.isValid()
Engine --> Consumer: ValidationResult\n(valid=true, warnings=[...])
note right: Pode ter warnings\nmas sem errors
else !result.isValid()
Engine --> Consumer: ValidationResult\n(valid=false, errors=[...])
end

deactivate Result
deactivate Engine

@enduml

```

---

## Diagrama de Sequência - Reprocessamento DLQ

```

@startuml Reprocessamento DLQ
!theme plain
autonumber

actor Operador
participant "DLQ\nController" as Controller
participant "DLQ\nService" as DLQService
participant "DLQ\nRepository" as DLQRepo
participant "Validation\nEngine" as Validation
participant "Persistence\nService" as Persistence
participant "PostgreSQL" as DB

Operador -> Controller: POST /api/v1/dlq/{id}/retry
activate Controller

Controller -> DLQService: reprocessEvent(dlqId)
activate DLQService

' 1. Buscar evento na DLQ
DLQService -> DLQRepo: findById(dlqId)
activate DLQRepo
DLQRepo -> DB: SELECT * FROM dlq_events WHERE id = ?
activate DB
DB --> DLQRepo: DLQEvent
deactivate DB
DLQRepo --> DLQService: Optional<DLQEvent>
deactivate DLQRepo

alt Evento não encontrado
DLQService --> Controller: throw EntityNotFoundException
Controller --> Operador: 404 Not Found
else Evento já reprocessado
DLQService -> DLQService: if (status == REPROCESSED)
DLQService --> Controller: throw IllegalStateException
Controller --> Operador: 400 Bad Request\n"Event already reprocessed"
else Excedeu max retries
DLQService -> DLQService: if (!canRetry())
DLQService --> Controller: throw MaxRetriesExceededException
Controller --> Operador: 400 Bad Request\n"Max retries exceeded"
else Pode reprocessar
' 2. Deserializar payload
DLQService -> DLQService: deserialize(eventPayload)\n→ EmployeeEventDTO

    ' 3. Revalidar (pode ter sido corrigido)
    DLQService -> Validation: validate(event)
    activate Validation
    Validation --> DLQService: ValidationResult
    deactivate Validation
    
    alt Ainda inválido
        DLQService -> DLQService: incrementRetryCount()
        DLQService -> DLQRepo: save(dlqEvent)
        activate DLQRepo
        DLQRepo -> DB: UPDATE dlq_events\nSET retry_count = retry_count + 1
        activate DB
        DB --> DLQRepo: OK
        deactivate DB
        DLQRepo --> DLQService: DLQEvent
        deactivate DLQRepo
        
        DLQService --> Controller: ReprocessResult\n(success=false, errors=[...])
        Controller --> Operador: 200 OK\n{"success": false}
        
    else Validação OK
        ' 4. Persistir evento corrigido
        DLQService -> Persistence: persist(event, kafkaOffset)
        activate Persistence
        Persistence -> DB: BEGIN TRANSACTION
        activate DB
        Persistence -> DB: INSERT/UPDATE employees
        Persistence -> DB: INSERT employees_history
        Persistence -> DB: COMMIT
        DB --> Persistence: OK
        deactivate DB
        Persistence --> DLQService: Success
        deactivate Persistence
        
        ' 5. Atualizar status DLQ
        DLQService -> DLQService: markAsReprocessed()
        DLQService -> DLQRepo: save(dlqEvent)
        activate DLQRepo
        DLQRepo -> DB: UPDATE dlq_events\nSET status = 'REPROCESSED'
        activate DB
        DB --> DLQRepo: OK
        deactivate DB
        DLQRepo --> DLQService: DLQEvent
        deactivate DLQRepo
        
        DLQService --> Controller: ReprocessResult\n(success=true)
        Controller --> Operador: 200 OK\n{"success": true}
    end
    end

deactivate DLQService
deactivate Controller

@enduml

```

---

## Padrões de Design Implementados

### 1. Strategy Pattern (Validações)

**Problema:** Diferentes regras de validação com mesma interface

**Solução:**
```

public interface ValidationRule {  // Strategy interface
void validate(EmployeeEventDTO event, ValidationResult result);
}

public class CpfFormatValidationRule implements ValidationRule { }
public class MinimumAgeValidationRule implements ValidationRule { }

```

**Benefícios:**
- ✅ Adicionar novas regras sem modificar engine
- ✅ Teste unitário isolado de cada regra
- ✅ Ordem de execução configurável

---

### 2. Template Method (AbstractValidationRule)

**Problema:** Código duplicado em todas as regras (try-catch, logs)

**Solução:**
```

public abstract class AbstractValidationRule implements ValidationRule {

    @Override
    public final void validate(...) {  // Template method
        try {
            doValidate(...);  // Hook method (implementado por subclasses)
        } catch (Exception e) {
            // Tratamento comum
        }
    }
    
    protected abstract void doValidate(...);  // Hook
    }

```

---

### 3. Chain of Responsibility (ValidationEngine)

**Problema:** Executar múltiplas validações em sequência com short-circuit

**Solução:**
```

public class ValidationEngine {
public ValidationResult validate(EmployeeEventDTO event) {
for (ValidationRule rule : rules) {
rule.validate(event, result);
if (result.hasError()) {
break;  // Short-circuit
}
}
return result;
}
}

```

---

### 4. Builder Pattern (DTOs e Entities)

**Problema:** Construtores com muitos parâmetros

**Solução:**
```

@Builder
public class EmployeeEventDTO {
// 15+ campos
}

// Uso
EmployeeEventDTO event = EmployeeEventDTO.builder()
.eventId("123")
.eventType("CREATE")
.cpf("12345678901")
.build();

```

---

### 5. Repository Pattern (Spring Data JPA)

**Problema:** Desacoplar persistência da lógica de negócio

**Solução:**
```

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
Optional<Employee> findBySourceId(String sourceId);
Optional<Employee> findByKafkaOffset(Long offset);
}

```

---

### 6. Dependency Injection (Spring)

**Problema:** Acoplamento forte entre componentes

**Solução:**
```

@Service
public class ValidationEngine {

    private final List<ValidationRule> rules;
    
    @Autowired  // Constructor injection
    public ValidationEngine(@Qualifier("allRules") List<ValidationRule> rules) {
        this.rules = rules;
    }
    }

```

---

## Métricas de Complexidade

| Classe | Métodos | Linhas | Complexidade Ciclomática | Cobertura |
|--------|---------|--------|--------------------------|-----------|
| **ValidationEngine** | 5 | 120 | 8 | 95% |
| **CpfFormatValidationRule** | 3 | 45 | 4 | 100% |
| **PersistenceService** | 7 | 180 | 12 | 88% |
| **KafkaConsumerService** | 4 | 95 | 6 | 92% |
| **DLQService** | 6 | 140 | 10 | 85% |

---

## Referências

- [Design Patterns: Elements of Reusable Object-Oriented Software](https://www.amazon.com/Design-Patterns-Elements-Reusable-Object-Oriented/dp/0201633612)
- [Effective Java (3rd Edition)](https://www.amazon.com/Effective-Java-Joshua-Bloch/dp/0134685997)
- [PlantUML Sequence Diagrams](https://plantuml.com/sequence-diagram)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

---

**Última atualização:** 2025-11-22  
**Autor:** Márcio Kuroki Gonçalves