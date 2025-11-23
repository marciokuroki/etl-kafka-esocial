# Manual do Desenvolvedor - Pipeline ETL eSocial

**Versão:** 1.0  
**Data:** 2025-11-22  
**Projeto:** Pipeline ETL eSocial  
**Público-alvo:** Desenvolvedores Java/Spring Boot

---

## 📋 Índice

1. [Setup do Ambiente de Desenvolvimento](#setup-do-ambiente-de-desenvolvimento)
2. [Estrutura do Projeto](#estrutura-do-projeto)
3. [Convenções de Código](#convenções-de-código)
4. [Padrões de Design Utilizados](#padrões-de-design-utilizados)
5. [Como Adicionar Validações](#como-adicionar-validações)
6. [Como Adicionar Tópicos Kafka](#como-adicionar-tópicos-kafka)
7. [APIs REST](#apis-rest)
8. [Testes](#testes)
9. [Build e Deploy](#build-e-deploy)
10. [Debugging](#debugging)

---

## Setup do Ambiente de Desenvolvimento

### Pré-requisitos

| Ferramenta | Versão | Obrigatório | Descrição |
|------------|--------|-------------|-----------|
| **Java JDK** | 21 (LTS) | ✅ Sim | OpenJDK ou Oracle JDK |
| **Maven** | 3.9+ | ✅ Sim | Build tool |
| **Docker** | 24.0+ | ✅ Sim | Containers |
| **Docker Compose** | 2.20+ | ✅ Sim | Orquestração |
| **Git** | 2.40+ | ✅ Sim | Controle de versão |
| **IntelliJ IDEA** | 2024.1+ | 🟡 Recomendado | IDE (ou VS Code) |
| **Postman** | Latest | 🟡 Opcional | Testar APIs |

---

### Instalação Passo-a-Passo

#### 1. Instalar Java 21 (OpenJDK)

**Linux/Mac:**
```


# SDKMAN (recomendado)

curl -s "https://get.sdkman.io" | bash
source "\$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.1-tem

# Verificar instalação

java -version

# Saída esperada: openjdk version "21.0.1"

```

**Windows:**
- Download: https://adoptium.net/temurin/releases/?version=21
- Instalar e adicionar ao PATH
- Verificar: `java -version`

---

#### 2. Instalar Maven

**Linux/Mac:**
```


# Via SDKMAN

sdk install maven 3.9.5

# Verificar

mvn -version

```

**Windows:**
- Download: https://maven.apache.org/download.cgi
- Extrair e adicionar `bin/` ao PATH
- Verificar: `mvn -version`

---

#### 3. Instalar Docker e Docker Compose

**Linux (Ubuntu/Debian):**
```


# Remover versões antigas

sudo apt remove docker docker-engine docker.io containerd runc

# Instalar Docker

curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Adicionar usuário ao grupo docker

sudo usermod -aG docker \$USER
newgrp docker

# Instalar Docker Compose

sudo apt install docker-compose-plugin

# Verificar

docker --version
docker compose version

```

**Mac:**
- Download Docker Desktop: https://www.docker.com/products/docker-desktop

**Windows:**
- Download Docker Desktop: https://www.docker.com/products/docker-desktop
- Requer WSL2

---

#### 4. Clonar Repositório

```


# Clone

git clone https://github.com/marciokuroki/etl-kafka-esocial.git
cd etl-kafka-esocial

# Checkout branch de desenvolvimento

git checkout sprint3

# Verificar estrutura

ls -la

# Saída: producer-service/ consumer-service/ docker-compose.yml ...

```

---

#### 5. Configurar IDE (IntelliJ IDEA)

1. **Abrir Projeto:**
   - `File → Open → Selecionar pasta raiz`
   - IntelliJ detecta automaticamente projeto Maven

2. **Configurar JDK:**
   - `File → Project Structure → Project SDK → Java 21`

3. **Importar Dependências Maven:**
   - IntelliJ executa `mvn dependency:resolve` automaticamente
   - Aguardar conclusão (barra de progresso no canto inferior)

4. **Instalar Plugins Recomendados:**
   - Lombok Plugin (obrigatório)
   - SonarLint (code quality)
   - Docker Plugin
   - Kubernetes (se trabalhar com K8s)

5. **Habilitar Annotation Processing:**
   - `Settings → Build → Compiler → Annotation Processors`
   - ✅ `Enable annotation processing`

---

#### 6. Compilar Serviços

```


# Producer Service

cd producer-service
mvn clean install -DskipTests

# Sucesso: BUILD SUCCESS

# Consumer Service

cd ../consumer-service
mvn clean install -DskipTests

# Sucesso: BUILD SUCCESS

cd ..

```

---

#### 7. Iniciar Infraestrutura (Docker Compose)

```


# Iniciar todos os containers

docker-compose up -d

# Verificar status (aguardar ~2 minutos)

docker-compose ps

# Logs dos serviços

docker-compose logs -f producer-service consumer-service

```

---

#### 8. Verificar Instalação

```


# Health checks

curl http://localhost:8081/actuator/health | jq
curl http://localhost:8082/actuator/health | jq

# Ambos devem retornar: {"status":"UP"}

```

✅ **Ambiente pronto para desenvolvimento!**

---

### Troubleshooting Setup

#### Problema: "JAVA_HOME not set"

```


# Linux/Mac

echo 'export JAVA_HOME=\$HOME/.sdkman/candidates/java/current' >> ~/.bashrc
source ~/.bashrc

# Verificar

echo \$JAVA_HOME

```

#### Problema: "Docker permission denied"

```


# Adicionar usuário ao grupo docker

sudo usermod -aG docker \$USER
newgrp docker

# Reiniciar sessão

```

#### Problema: "Port already in use"

```


# Verificar o que está usando a porta

sudo lsof -i :8081

# Matar processo

kill -9 <PID>

```

---

## Estrutura do Projeto

### Visão Geral

```

etl-kafka-esocial/
├── producer-service/          \# Serviço Producer (CDC + Kafka)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/esocial/producer/
│   │   │   │   ├── config/        \# Configurações Spring/Kafka
│   │   │   │   ├── service/       \# Lógica de negócio (CDC)
│   │   │   │   ├── kafka/         \# Kafka Producer
│   │   │   │   ├── model/         \# Entidades JPA
│   │   │   │   ├── dto/           \# DTOs de eventos
│   │   │   │   └── ProducerServiceApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── logback-spring.xml
│   │   └── test/                  \# Testes unitários (18 testes)
│   ├── pom.xml
│   └── README.md
│
├── consumer-service/          \# Serviço Consumer (Validação + Persistência)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/esocial/consumer/
│   │   │   │   ├── config/        \# Configurações Spring/Kafka
│   │   │   │   ├── service/       \# Lógica de negócio
│   │   │   │   ├── kafka/         \# Kafka Consumer
│   │   │   │   ├── validation/    \# Motor de validações
│   │   │   │   │   ├── ValidationEngine.java
│   │   │   │   │   ├── ValidationRule.java (interface)
│   │   │   │   │   ├── AbstractValidationRule.java
│   │   │   │   │   ├── structural/  \# 6 regras estruturais
│   │   │   │   │   └── business/    \# 5 regras de negócio
│   │   │   │   ├── model/         \# Entidades JPA
│   │   │   │   ├── dto/           \# DTOs
│   │   │   │   ├── repository/    \# Spring Data JPA
│   │   │   │   ├── controller/    \# REST Controllers
│   │   │   │   └── ConsumerServiceApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── logback-spring.xml
│   │   │       └── db/migration/  \# Flyway migrations
│   │   └── test/                  \# Testes (35 unitários + 23 E2E)
│   ├── pom.xml
│   └── README.md
│
├── docs/                      \# Documentação
│   ├── architecture/
│   │   ├── c4-level1-context.md
│   │   ├── c4-level2-containers.md
│   │   ├── c4-level3-components.md
│   │   ├── c4-level4-code.md
│   │   └── deployment-diagram.md
│   ├── adr/                   \# ADRs (7 documentos)
│   ├── sprint1/               \# Retrospectivas
│   ├── sprint2/
│   ├── sprint3/
│   ├── operations/
│   │   └── OPERATIONS_MANUAL.md
│   ├── DEVELOPER_GUIDE.md     \# Este arquivo
│   └── API_DOCUMENTATION.md
│
├── scripts/                   \# Scripts de automação
│   ├── setup-ci-cd.sh
│   ├── health-check.sh
│   ├── backup-postgresql.sh
│   └── deploy.sh
│
├── prometheus/                \# Configurações Prometheus
│   ├── prometheus.yml
│   └── alert-rules.yml
│
├── alertmanager/              \# Configurações Alertmanager
│   └── alertmanager.yml
│
├── grafana/                   \# Dashboards Grafana
│   └── dashboards/
│
├── docker-compose.yml         \# Orquestração (14 containers)
├── .gitignore
├── README.md
└── LICENSE

```

---

### Módulos Principais

#### Producer Service

| Pacote | Responsabilidade | Classes Principais |
|--------|------------------|-------------------|
| `config` | Configuração Spring/Kafka | `KafkaProducerConfig`, `DatabaseConfig` |
| `service` | Lógica CDC | `CDCPollingService`, `EventMappingService` |
| `kafka` | Publicação Kafka | `KafkaProducerService` |
| `model` | Entidades JPA | `Employee` (origem) |
| `dto` | DTOs de eventos | `EmployeeEventDTO` |

**Fluxo:**
```

CDCPollingService (a cada 5s)
→ Detecta mudanças no PostgreSQL (source.employees)
→ EventMappingService.mapToEvent()
→ KafkaProducerService.publish(topic, event)

```

---

#### Consumer Service

| Pacote | Responsabilidade | Classes Principais |
|--------|------------------|-------------------|
| `config` | Configuração Spring/Kafka | `KafkaConsumerConfig`, `DatabaseConfig` |
| `kafka` | Consumo Kafka | `KafkaConsumerService` |
| `validation` | Motor de validações | `ValidationEngine`, 11 regras |
| `service` | Persistência + DLQ | `PersistenceService`, `DLQService` |
| `model` | Entidades JPA | `Employee`, `EmployeeHistory`, `DLQEvent` |
| `repository` | Spring Data JPA | `EmployeeRepository`, `DLQRepository` |
| `controller` | REST APIs | `ValidationController` |

**Fluxo:**
```

KafkaConsumerService.consume(event)
→ ValidationEngine.validate(event)
→ [PASS] PersistenceService.persist(employee) + audit
→ [FAIL] DLQService.save(event, errors)

```

---

## Convenções de Código

### Padrão de Nomenclatura

#### Pacotes

```

com.esocial.{service}.{categoria}

Exemplos:

- com.esocial.producer.config
- com.esocial.consumer.validation.structural

```

#### Classes

| Tipo | Padrão | Exemplo |
|------|--------|---------|
| **Service** | `{Funcionalidade}Service` | `CDCPollingService` |
| **Controller** | `{Entidade}Controller` | `ValidationController` |
| **Repository** | `{Entidade}Repository` | `EmployeeRepository` |
| **DTO** | `{Entidade}{Tipo}DTO` | `EmployeeEventDTO` |
| **Entity** | `{Entidade}` | `Employee` |
| **Config** | `{Tecnologia}Config` | `KafkaProducerConfig` |
| **Rule** | `{Validacao}ValidationRule` | `CpfFormatValidationRule` |

#### Métodos

```

// Convenção: verbo + substantivo + complemento
public void processEvent(EmployeeEventDTO event)
public Employee findBySourceId(String sourceId)
public boolean validateCpfFormat(String cpf)
public void publishToKafka(String topic, EmployeeEventDTO event)

```

#### Variáveis

```

// CamelCase para variáveis
private String employeeName;
private LocalDate admissionDate;

// UPPER_CASE para constantes
private static final int MINIMUM_AGE = 16;
private static final String TOPIC_CREATE = "employee-create";

```

---

### Anotações Lombok

```

@Data               // Gera getters, setters, toString, equals, hashCode
@Builder            // Padrão Builder
@NoArgsConstructor  // Construtor vazio (JPA requer)
@AllArgsConstructor // Construtor com todos os campos
@Slf4j              // Logger (log.info, log.error)

```

**Exemplo completo:**
```

@Entity
@Table(name = "employees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Employee {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    private String sourceId;
    private String cpf;
    // ...
    }

```

---

### Anotações Spring Boot

```

@Service            // Marca classe como service (bean gerenciado)
@Repository         // Marca interface como repository (DAO)
@RestController     // Controller REST
@Configuration      // Classe de configuração
@Component          // Bean genérico
@Autowired          // Injeção de dependência (preferir constructor injection)

```

---

### Estrutura de Commits (Conventional Commits)

```

<tipo>(<escopo>): <descrição curta>

<corpo detalhado (opcional)>

<footer (opcional)>

```

**Tipos:**
- `feat`: Nova funcionalidade
- `fix`: Correção de bug
- `docs`: Documentação
- `test`: Adicionar/corrigir testes
- `refactor`: Refatoração (sem alterar comportamento)
- `chore`: Tarefas de manutenção
- `ci`: Mudanças no CI/CD

**Exemplos:**
```

feat(consumer): adicionar validação de PIS
fix(producer): corrigir memory leak no CDC polling
docs(adr): adicionar ADR-0007 sobre validações
test(consumer): adicionar testes para MinimumAgeRule
refactor(validation): extrair lógica comum para AbstractValidationRule
chore(deps): atualizar Spring Boot para 3.2.1

```

---

### Estilo de Código (Google Java Style Guide)

```

// Indentação: 4 espaços (não tabs)
public class Example {
private String field;  // 4 espaços

    public void method() {
        if (condition) {  // 4 espaços
            doSomething();  // 8 espaços
        }
    }
    }

// Chaves sempre em nova linha (Allman style)
if (condition)
{
// code
}

// Linhas: máximo 120 caracteres

```

**Configurar no IntelliJ:**
- `Settings → Editor → Code Style → Java`
- Importar: `config/intellij-code-style.xml`

---

## Padrões de Design Utilizados

### 1. Strategy Pattern (Validações)

**Problema:** Diferentes regras de validação com mesma interface

**Implementação:**
```

// Interface comum
public interface ValidationRule {
void validate(EmployeeEventDTO event, ValidationResult result);
String getRuleName();
ValidationSeverity getSeverity();
}

// Implementações concretas
@Component
public class CpfFormatValidationRule implements ValidationRule {
@Override
public void validate(EmployeeEventDTO event, ValidationResult result) {
// Lógica específica de validação CPF
}
}

@Component
public class MinimumAgeValidationRule implements ValidationRule {
@Override
public void validate(EmployeeEventDTO event, ValidationResult result) {
// Lógica específica de validação idade
}
}

// Engine que usa as strategies
@Service
public class ValidationEngine {
private final List<ValidationRule> rules;

    @Autowired
    public ValidationEngine(List<ValidationRule> rules) {
        this.rules = rules;  // Spring injeta TODAS as implementações
    }
    
    public ValidationResult validate(EmployeeEventDTO event) {
        for (ValidationRule rule : rules) {
            rule.validate(event, result);
            if (result.hasError()) break;  // Fail-fast
        }
        return result;
    }
    }

```

**Benefícios:**
- ✅ Adicionar nova regra = criar nova classe (Open/Closed Principle)
- ✅ Teste unitário isolado por regra
- ✅ Sem modificar ValidationEngine

---

### 2. Template Method (AbstractValidationRule)

**Problema:** Código duplicado em todas as regras (try-catch, logs)

**Implementação:**
```

public abstract class AbstractValidationRule implements ValidationRule {
protected final String ruleName;
protected final ValidationSeverity severity;

    @Override
    public final void validate(EmployeeEventDTO event, ValidationResult result) {
        try {
            // Template method (comum a todas as regras)
            doValidate(event, result);  // Chama método abstrato
        } catch (Exception e) {
            // Tratamento de erro comum
            result.addError(ruleName, "system", "Erro interno: " + e.getMessage(), severity);
        }
    }
    
    // Hook method (implementado pelas subclasses)
    protected abstract void doValidate(EmployeeEventDTO event, ValidationResult result);
    }

// Uso
@Component
public class CpfFormatValidationRule extends AbstractValidationRule {
@Override
protected void doValidate(EmployeeEventDTO event, ValidationResult result) {
// Apenas lógica específica de CPF
if (!isValidCpf(event.getCpf())) {
result.addError(getRuleName(), "cpf", "CPF inválido", getSeverity());
}
}
}

```

**Benefícios:**
- ✅ Elimina duplicação
- ✅ Tratamento de erros consistente
- ✅ Subclasses focam apenas na lógica específica

---

### 3. Chain of Responsibility (Validações em Camadas)

**Problema:** Executar validações em ordem específica com fail-fast

**Implementação:**
```

@Service
public class ValidationEngine {
private final List<ValidationRule> structuralRules;
private final List<ValidationRule> businessRules;

    public ValidationResult validate(EmployeeEventDTO event) {
        ValidationResult result = new ValidationResult();
        
        // Camada 1: Estrutural
        executeRules(structuralRules, event, result);
        if (result.hasError()) return result;  // ← Chain para aqui
        
        // Camada 2: Negócio
        executeRules(businessRules, event, result);
        if (result.hasError()) return result;  // ← Chain para aqui
        
        return result;
    }
    }

```

---

### 4. Repository Pattern (Spring Data JPA)

**Problema:** Desacoplar acesso a dados da lógica de negócio

**Implementação:**
```

// Interface (apenas declaração)
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
Optional<Employee> findBySourceId(String sourceId);
Optional<Employee> findByKafkaOffset(Long offset);

    @Query("SELECT e FROM Employee e WHERE e.status = :status")
    List<Employee> findByStatus(@Param("status") String status);
    }

// Uso no service
@Service
public class PersistenceService {
@Autowired
private EmployeeRepository employeeRepository;

    public void persist(EmployeeEventDTO event) {
        Employee employee = employeeRepository.findBySourceId(event.getSourceId())
            .orElse(new Employee());
        
        // Atualizar campos
        employee.setCpf(event.getCpf());
        // ...
        
        employeeRepository.save(employee);  // INSERT ou UPDATE
    }
    }

```

---

### 5. Builder Pattern (DTOs e Entities)

**Problema:** Construtores com muitos parâmetros

**Implementação:**
```

@Builder
public class EmployeeEventDTO {
private String eventId;
private String eventType;
private String sourceId;
private String cpf;
// ... 15+ campos
}

// Uso
EmployeeEventDTO event = EmployeeEventDTO.builder()
.eventId(UUID.randomUUID().toString())
.eventType("CREATE")
.sourceId("EMP100")
.cpf("12345678901")
.fullName("João Silva")
.build();

```

---

## Como Adicionar Validações

### Passo-a-Passo: Adicionar Nova Validação

#### Cenário: Validar formato de Email

**1. Criar classe de validação**

```

// consumer-service/src/main/java/com/esocial/consumer/validation/structural/EmailFormatValidationRule.java

package com.esocial.consumer.validation.structural;

import com.esocial.consumer.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.AbstractValidationRule;
import com.esocial.consumer.validation.ValidationResult;
import com.esocial.consumer.validation.ValidationSeverity;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Order(7)  // Ordem de execução (após PisFormatValidationRule que é 6)
public class EmailFormatValidationRule extends AbstractValidationRule {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    public EmailFormatValidationRule() {
        super(
            "INVALID_EMAIL_FORMAT",  // Nome da regra (para logs)
            ValidationSeverity.ERROR,  // ERROR bloqueia, WARNING não bloqueia
            7  // Ordem de execução
        );
    }
    
    @Override
    protected void doValidate(EmployeeEventDTO event, ValidationResult result) {
        String email = event.getEmail();
        
        // Email é opcional, então só valida se informado
        if (email == null || email.isEmpty()) {
            return;  // Válido (campo opcional)
        }
        
        // Validar formato
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            result.addError(
                getRuleName(),
                "email",
                String.format("Email '%s' possui formato inválido", email),
                getSeverity()
            );
        }
    }
    }

```

**2. Adicionar campo email no DTO**

```

// consumer-service/src/main/java/com/esocial/consumer/dto/EmployeeEventDTO.java

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEventDTO {
// ... campos existentes

    @JsonProperty("email")  // Mapeia snake_case do JSON
    private String email;  // ← Novo campo
    }

```

**3. Adicionar campo na entidade**

```

// consumer-service/src/main/java/com/esocial/consumer/model/Employee.java

@Entity
@Table(name = "employees")
@Data
public class Employee {
// ... campos existentes

    @Column(name = "email", length = 200)
    private String email;  // ← Novo campo
    }

```

**4. Criar migration Flyway**

```

-- consumer-service/src/main/resources/db/migration/V4__add_email_column.sql

ALTER TABLE public.employees
ADD COLUMN email VARCHAR(200);

ALTER TABLE audit.employees_history
ADD COLUMN email VARCHAR(200);

CREATE INDEX idx_employees_email ON public.employees(email);

```

**5. Adicionar teste unitário**

```

// consumer-service/src/test/java/com/esocial/consumer/validation/structural/EmailFormatValidationRuleTest.java

package com.esocial.consumer.validation.structural;

import com.esocial.consumer.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailFormatValidationRuleTest {

    private final EmailFormatValidationRule rule = new EmailFormatValidationRule();
    
    @Test
    void shouldAcceptValidEmail() {
        // Arrange
        EmployeeEventDTO event = EmployeeEventDTO.builder()
            .email("joao.silva@empresa.com.br")
            .build();
        ValidationResult result = new ValidationResult();
        
        // Act
        rule.validate(event, result);
        
        // Assert
        assertThat(result.isValid()).isTrue();
    }
    
    @Test
    void shouldRejectInvalidEmail() {
        // Arrange
        EmployeeEventDTO event = EmployeeEventDTO.builder()
            .email("invalid-email")  // Sem @ e domínio
            .build();
        ValidationResult result = new ValidationResult();
        
        // Act
        rule.validate(event, result);
        
        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getRuleName())
            .isEqualTo("INVALID_EMAIL_FORMAT");
    }
    
    @Test
    void shouldAcceptNullEmail() {
        // Arrange (email opcional)
        EmployeeEventDTO event = EmployeeEventDTO.builder()
            .email(null)
            .build();
        ValidationResult result = new ValidationResult();
        
        // Act
        rule.validate(event, result);
        
        // Assert
        assertThat(result.isValid()).isTrue();
    }
    }

```

**6. Executar testes**

```

cd consumer-service
mvn test -Dtest=EmailFormatValidationRuleTest

# Resultado esperado:

# Tests run: 3, Failures: 0, Errors: 0, Skipped: 0

```

**7. Rebuild e restart**

```


# Rebuild com novo código

mvn clean install -DskipTests

# Restart consumer

docker-compose restart consumer-service

# Verificar logs

docker-compose logs -f consumer-service | grep "EmailFormatValidationRule"

```

✅ **Nova validação adicionada com sucesso!**

---

### Tipos de Severidade

```

public enum ValidationSeverity {
ERROR,    // Bloqueia processamento (vai para DLQ)
WARNING,  // Permite processamento (apenas log)
INFO      // Informativo (métricas)
}

```

**Quando usar cada uma:**

| Severidade | Quando Usar | Exemplo |
|------------|-------------|---------|
| **ERROR** | Dados incorretos que impedem processamento | CPF inválido, idade < 16 anos |
| **WARNING** | Dados suspeitos mas aceitáveis | Salário abaixo do mínimo (pode ser aprendiz) |
| **INFO** | Informações estatísticas | Campo opcional não preenchido |

---

## Como Adicionar Tópicos Kafka

### Passo-a-Passo: Adicionar Novo Tópico

#### Cenário: Criar tópico `employee-termination` para demissões

**1. Atualizar configuração Kafka (Producer)**

```


# producer-service/src/main/resources/application.yml

kafka:
topics:
employee-create: "employee-create"
employee-update: "employee-update"
employee-delete: "employee-delete"
employee-termination: "employee-termination"  \# ← Novo tópico

```

**2. Atualizar KafkaProducerConfig**

```

// producer-service/src/main/java/com/esocial/producer/config/KafkaProducerConfig.java

@Configuration
public class KafkaProducerConfig {

    @Value("${kafka.topics.employee-create}")
    private String employeeCreateTopic;
    
    @Value("${kafka.topics.employee-update}")
    private String employeeUpdateTopic;
    
    @Value("${kafka.topics.employee-delete}")
    private String employeeDeleteTopic;
    
    @Value("${kafka.topics.employee-termination}")  // ← Novo
    private String employeeTerminationTopic;
    
    @Bean
    public NewTopic employeeTerminationTopic() {  // ← Novo bean
        return TopicBuilder.name(employeeTerminationTopic)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")  // 7 dias
            .build();
    }
    }

```

**3. Atualizar lógica de publicação**

```

// producer-service/src/main/java/com/esocial/producer/service/CDCPollingService.java

@Service
public class CDCPollingService {

    @Value("${kafka.topics.employee-termination}")
    private String employeeTerminationTopic;
    
    public void processChanges() {
        List<Employee> changes = detectChanges();
        
        for (Employee employee : changes) {
            String topic;
            
            if (employee.getTerminationDate() != null && 
                employee.getStatus().equals("TERMINATED")) {
                // Demissão
                topic = employeeTerminationTopic;  // ← Novo tópico
            } else if (isNewRecord(employee)) {
                topic = employeeCreateTopic;
            } else {
                topic = employeeUpdateTopic;
            }
            
            EmployeeEventDTO event = mapToEvent(employee);
            kafkaProducerService.publish(topic, event);
        }
    }
    }

```

**4. Atualizar Consumer para escutar novo tópico**

```


# consumer-service/src/main/resources/application.yml

kafka:
topics:
- "employee-create"
- "employee-update"
- "employee-delete"
- "employee-termination"  \# ← Adicionar à lista

```

**5. Adicionar listener no Consumer**

```

// consumer-service/src/main/java/com/esocial/consumer/kafka/KafkaConsumerService.java

@Service
public class KafkaConsumerService {

    @KafkaListener(
        topics = "employee-termination",  // ← Novo listener
        groupId = "${kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTerminationEvent(
        @Payload EmployeeEventDTO event,
        @Header(KafkaHeaders.OFFSET) Long offset,
        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition
    ) {
        log.info("Consumindo evento de DEMISSÃO: sourceId={}, offset={}, partition={}", 
                 event.getSourceId(), offset, partition);
        
        processEvent(event, offset, partition);
    }
    }

```

**6. Criar tópico manualmente (ou deixar auto-create)**

```


# Criar tópico manualmente

docker exec esocial-kafka-broker-1 kafka-topics \
--bootstrap-server localhost:9092 \
--create \
--topic employee-termination \
--partitions 3 \
--replication-factor 3

# Verificar

docker exec esocial-kafka-broker-1 kafka-topics \
--bootstrap-server localhost:9092 \
--describe \
--topic employee-termination

```

**7. Testar publicação**

```


# 1. Inserir colaborador demitido

docker exec -it esocial-postgres-db psql -U esocial_user -d esocial -c \
"UPDATE source.employees SET status = 'TERMINATED', termination_date = '2025-11-22' WHERE employee_id = 'EMP100';"

# 2. Aguardar CDC (5 segundos)

# 3. Verificar Kafka UI

# http://localhost:8090 → Topics → employee-termination → Messages

# 4. Verificar logs do consumer

docker-compose logs consumer-service | grep "DEMISSÃO"

```

✅ **Novo tópico adicionado com sucesso!**

---

### Configurações Avançadas de Tópico

```

@Bean
public NewTopic customTopic() {
return TopicBuilder.name("my-topic")
.partitions(6)  // Mais partições = mais paralelismo
.replicas(3)    // Replicação (mínimo 2, ideal 3)
.config(TopicConfig.RETENTION_MS_CONFIG, "86400000")  // 1 dia
.config(TopicConfig.COMPRESSION_TYPE_CONFIG, "snappy")  // Compressão
.config(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, "10485760")  // 10 MB
.build();
}

```

---

## APIs REST

### Documentação das APIs (Consumer Service)

#### 1. Listar Erros de Validação

**Endpoint:** `GET /api/v1/validation/errors`

**Descrição:** Retorna lista de erros de validação persistidos.

**Parâmetros Query:**
- `page` (optional): Número da página (default: 0)
- `size` (optional): Tamanho da página (default: 20)
- `ruleName` (optional): Filtrar por regra específica

**Exemplo Request:**
```

curl http://localhost:8082/api/v1/validation/errors?page=0\&size=10\&ruleName=INVALID_CPF_FORMAT | jq

```

**Exemplo Response:**
```

[
{
"id": 123,
"eventId": "evt-456",
"sourceId": "EMP100",
"validationRule": "INVALID_CPF_FORMAT",
"errorMessage": "CPF '123' deve ter 11 dígitos numéricos",
"severity": "ERROR",
"fieldName": "cpf",
"fieldValue": "123",
"createdAt": "2025-11-22T10:30:00"
}
]

```

---

#### 2. Dashboard de Validação

**Endpoint:** `GET /api/v1/validation/dashboard`

**Descrição:** Retorna estatísticas consolidadas de validação.

**Exemplo Request:**
```

curl http://localhost:8082/api/v1/validation/dashboard | jq

```

**Exemplo Response:**
```

{
"totalEvents": 15000,
"successfulEvents": 13500,
"failedEvents": 1500,
"successRate": 90.0,
"averageProcessingTime": "85ms",
"errorsByRule": {
"INVALID_CPF_FORMAT": 450,
"MINIMUM_AGE_VIOLATION": 320,
"FUTURE_DATE": 180,
"BELOW_MINIMUM_SALARY": 550
},
"dlqStatistics": {
"pendingEvents": 87,
"reprocessedEvents": 1200,
"failedEvents": 213
}
}

```

---

#### 3. Listar Eventos na DLQ

**Endpoint:** `GET /api/v1/validation/dlq`

**Descrição:** Retorna eventos na Dead Letter Queue.

**Parâmetros Query:**
- `status` (optional): PENDING | REPROCESSING | REPROCESSED | FAILED
- `page` (optional): Número da página (default: 0)
- `size` (optional): Tamanho da página (default: 20)

**Exemplo Request:**
```

curl "http://localhost:8082/api/v1/validation/dlq?status=PENDING\&page=0\&size=10" | jq

```

**Exemplo Response:**
```

[
{
"id": 1,
"eventId": "evt-789",
"eventType": "CREATE",
"errorMessage": "INVALID_CPF_FORMAT: CPF '123' inválido",
"retryCount": 0,
"maxRetries": 3,
"status": "PENDING",
"canRetry": true,
"createdAt": "2025-11-22T10:45:00"
}
]

```

---

#### 4. Reprocessar Evento DLQ

**Endpoint:** `POST /api/v1/validation/dlq/{id}/retry`

**Descrição:** Tenta reprocessar evento específico da DLQ.

**Path Parameters:**
- `id`: ID do evento na DLQ

**Exemplo Request:**
```

curl -X POST http://localhost:8082/api/v1/validation/dlq/1/retry | jq

```

**Exemplo Response (Sucesso):**
```

{
"success": true,
"message": "Event reprocessed successfully",
"eventId": "evt-789",
"retriesRemaining": 2
}

```

**Exemplo Response (Falha):**
```

{
"success": false,
"message": "Event still invalid",
"errors": [
{
"ruleName": "INVALID_CPF_FORMAT",
"fieldName": "cpf",
"message": "CPF '123' inválido"
}
],
"retriesRemaining": 2
}

```

---

### Collection Postman

Crie o arquivo `docs/postman/esocial-api-collection.json`:

```

{
"info": {
"name": "Pipeline ETL eSocial - APIs",
"schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
},
"item": [
{
"name": "Health Check - Producer",
"request": {
"method": "GET",
"header": [],
"url": {
"raw": "http://localhost:8081/actuator/health",
"host": ["localhost"],
"port": "8081",
"path": ["actuator", "health"]
}
}
},
{
"name": "Health Check - Consumer",
"request": {
"method": "GET",
"header": [],
"url": {
"raw": "http://localhost:8082/actuator/health",
"host": ["localhost"],
"port": "8082",
"path": ["actuator", "health"]
}
}
},
{
"name": "Validation Errors - All",
"request": {
"method": "GET",
"header": [],
"url": {
"raw": "http://localhost:8082/api/v1/validation/errors?page=0\&size=20",
"host": ["localhost"],
"port": "8082",
"path": ["api", "v1", "validation", "errors"],
"query": [
{"key": "page", "value": "0"},
{"key": "size", "value": "20"}
]
}
}
},
{
"name": "Validation Dashboard",
"request": {
"method": "GET",
"header": [],
"url": {
"raw": "http://localhost:8082/api/v1/validation/dashboard",
"host": ["localhost"],
"port": "8082",
"path": ["api", "v1", "validation", "dashboard"]
}
}
},
{
"name": "DLQ Events - Pending",
"request": {
"method": "GET",
"header": [],
"url": {
"raw": "http://localhost:8082/api/v1/validation/dlq?status=PENDING",
"host": ["localhost"],
"port": "8082",
"path": ["api", "v1", "validation", "dlq"],
"query": [{"key": "status", "value": "PENDING"}]
}
}
},
{
"name": "DLQ Retry Event",
"request": {
"method": "POST",
"header": [],
"url": {
"raw": "http://localhost:8082/api/v1/validation/dlq/1/retry",
"host": ["localhost"],
"port": "8082",
"path": ["api", "v1", "validation", "dlq", "1", "retry"]
}
}
}
]
}

```

**Importar no Postman:**
1. Abrir Postman
2. File → Import
3. Selecionar `esocial-api-collection.json`
4. Collection aparece no sidebar esquerdo

---

## Testes

### Estrutura de Testes

```

consumer-service/src/test/java/
├── unit/                         \# Testes unitários (35 testes)
│   ├── validation/
│   │   ├── structural/
│   │   │   ├── CpfFormatValidationRuleTest.java
│   │   │   ├── PisFormatValidationRuleTest.java
│   │   │   └── ...
│   │   ├── business/
│   │   │   ├── MinimumAgeValidationRuleTest.java
│   │   │   └── ...
│   │   └── ValidationEngineTest.java
│   ├── service/
│   │   ├── PersistenceServiceTest.java
│   │   └── DLQServiceTest.java
│   └── kafka/
│       └── KafkaConsumerServiceTest.java
│
└── integration/                  \# Testes E2E (23 testes)
├── AbstractIntegrationTest.java
├── EmployeeInsertE2ETest.java
├── EmployeeUpdateE2ETest.java
├── EmployeeDeleteE2ETest.java
├── ValidationE2ETest.java
├── DLQReprocessE2ETest.java
└── FullPipelineE2ETest.java

```

---

### Executar Testes

```


# Todos os testes unitários (rápido: ~30 segundos)

mvn test -Dtest=*Test

# Todos os testes E2E (lento: ~2 minutos)

mvn verify -Pe2e-tests

# Teste específico

mvn test -Dtest=CpfFormatValidationRuleTest

# Com relatório de cobertura

mvn clean test jacoco:report

# Ver relatório HTML

open target/site/jacoco/index.html

```

---

### Exemplo de Teste Unitário

```

package com.esocial.consumer.validation.structural;

import com.esocial.consumer.dto.EmployeeEventDTO;
import com.esocial.consumer.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CPF Format Validation Rule Tests")
class CpfFormatValidationRuleTest {

    private CpfFormatValidationRule rule;
    
    @BeforeEach
    void setUp() {
        rule = new CpfFormatValidationRule();
    }
    
    @Test
    @DisplayName("Should accept valid CPF with 11 digits")
    void shouldAcceptValidCpf() {
        // Arrange
        EmployeeEventDTO event = EmployeeEventDTO.builder()
            .cpf("12345678901")
            .build();
        ValidationResult result = new ValidationResult();
        
        // Act
        rule.validate(event, result);
        
        // Assert
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }
    
    @Test
    @DisplayName("Should reject CPF with less than 11 digits")
    void shouldRejectShortCpf() {
        // Arrange
        EmployeeEventDTO event = EmployeeEventDTO.builder()
            .cpf("123456789")  // 9 dígitos
            .build();
        ValidationResult result = new ValidationResult();
        
        // Act
        rule.validate(event, result);
        
        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getRuleName())
            .isEqualTo("INVALID_CPF_FORMAT");
        assertThat(result.getErrors().get(0).getFieldName())
            .isEqualTo("cpf");
    }
    
    @Test
    @DisplayName("Should reject null CPF")
    void shouldRejectNullCpf() {
        // Arrange
        EmployeeEventDTO event = EmployeeEventDTO.builder()
            .cpf(null)
            .build();
        ValidationResult result = new ValidationResult();
        
        // Act
        rule.validate(event, result);
        
        // Assert
        assertThat(result.isValid()).isFalse();
    }
    }

```

---

### Exemplo de Teste E2E

```

package com.esocial.consumer.integration;

import com.esocial.consumer.dto.EmployeeEventDTO;
import com.esocial.consumer.model.Employee;
import com.esocial.consumer.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
class EmployeeInsertE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, EmployeeEventDTO> kafkaTemplate;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Test
    void shouldInsertEmployeeWhenValidEventIsPublished() {
        // Arrange
        EmployeeEventDTO event = EmployeeEventDTO.builder()
            .eventId("evt-001")
            .eventType("CREATE")
            .sourceId("EMP100")
            .cpf("12345678901")
            .pis("10011223344")
            .fullName("João da Silva")
            .birthDate(LocalDate.of(1990, 1, 15))
            .admissionDate(LocalDate.of(2024, 1, 10))
            .salary(new BigDecimal("5500.00"))
            .status("ACTIVE")
            .build();
        
        // Act
        kafkaTemplate.send("employee-create", event.getSourceId(), event);
        
        // Assert
        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   Optional<Employee> employee = employeeRepository.findBySourceId("EMP100");
                   
                   assertThat(employee).isPresent();
                   assertThat(employee.get().getCpf()).isEqualTo("12345678901");
                   assertThat(employee.get().getFullName()).isEqualTo("João da Silva");
                   assertThat(employee.get().getVersion()).isEqualTo(1);
               });
    }
    }

```

---

## Build e Deploy

### Build Local

```


# Build completo com testes

mvn clean install

# Build sem testes (rápido)

mvn clean install -DskipTests

# Build com perfil específico

mvn clean install -Pproduction

# Gerar relatórios

mvn clean test jacoco:report spotbugs:check

```

---

### Docker Build

```


# Build imagem do Producer

cd producer-service
docker build -t esocial-producer:1.0.0 .

# Build imagem do Consumer

cd ../consumer-service
docker build -t esocial-consumer:1.0.0 .

# Push para Docker Hub (se configurado)

docker tag esocial-producer:1.0.0 marciokuroki/esocial-producer:1.0.0
docker push marciokuroki/esocial-producer:1.0.0

```

---

### Deploy (Docker Compose)

```


# Deploy simples (desenvolvimento)

docker-compose up -d

# Deploy com rebuild

docker-compose up -d --build

# Deploy apenas de um serviço

docker-compose up -d --no-deps consumer-service

# Rollback (versão anterior)

git checkout <commit-anterior>
docker-compose up -d --build

```

---

## Debugging

### Debug via IntelliJ IDEA

**1. Configurar Remote Debug:**

Adicione no `docker-compose.yml`:

```

consumer-service:
environment:
- JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
ports:
- "8082:8082"
- "5005:5005"  \# ← Porta de debug

```

**2. Criar configuração no IntelliJ:**
- Run → Edit Configurations
- `+` → Remote JVM Debug
- Host: `localhost`
- Port: `5005`
- Debugger mode: `Attach to remote JVM`
- Salvar como "Consumer Remote Debug"

**3. Iniciar debug:**
- Colocar breakpoints no código
- Run → Debug "Consumer Remote Debug"
- Executar requisição que aciona o breakpoint

---

### Logs Estruturados

**Configuração Logback:**

```

<!-- consumer-service/src/main/resources/logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            ```
            <customFields>{"service":"consumer-service"}</customFields>
            ```
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
    
    <logger name="com.esocial.consumer" level="DEBUG"/>
    <logger name="org.springframework.kafka" level="WARN"/>
</configuration>
```

**Buscar logs:**

```


# Logs em tempo real

docker-compose logs -f consumer-service

# Buscar por correlation ID

docker-compose logs consumer-service | grep "correlationId=abc-123"

# Buscar por ERROR

docker-compose logs consumer-service | grep "ERROR"

# Últimas 100 linhas

docker-compose logs --tail=100 consumer-service

```

---

## Anexos

### Ferramentas Recomendadas

| Ferramenta | Descrição | Link |
|------------|-----------|------|
| **IntelliJ IDEA** | IDE Java | https://www.jetbrains.com/idea/ |
| **VS Code** | Editor alternativo | https://code.visualstudio.com/ |
| **Postman** | Testar APIs | https://www.postman.com/ |
| **DBeaver** | Cliente PostgreSQL | https://dbeaver.io/ |
| **k9s** | Gerenciar Kubernetes | https://k9scli.io/ |
| **Lens** | Kubernetes IDE | https://k8slens.dev/ |

---

### Recursos de Aprendizado

| Recurso | Descrição | Link |
|---------|-----------|------|
| **Spring Boot Docs** | Documentação oficial | https://docs.spring.io/spring-boot/ |
| **Apache Kafka Docs** | Guia completo Kafka | https://kafka.apache.org/documentation/ |
| **Baeldung** | Tutoriais Spring/Java | https://www.baeldung.com/ |
| **Effective Java** | Livro (Joshua Bloch) | Amazon |
| **Clean Code** | Livro (Robert C. Martin) | Amazon |

---

## Changelog

| Versão | Data | Autor | Mudanças |
|--------|------|-------|----------|
| 1.0 | 2025-11-22 | Márcio Kuroki | Criação inicial |

---

**Última atualização:** 2025-11-22  
**Autor:** Márcio Kuroki Gonçalves  
**Contato:** marciokuroki@gmail.com