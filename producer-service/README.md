```markdown
# Producer Service

[![Tests](https://img.shields.io/badge/tests-18%20passed-brightgreen)]()
[![Coverage](https://img.shields.io/badge/coverage-82%25-brightgreen)]()
[![Build](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Java](https://img.shields.io/badge/java-21-blue)]()
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.2.0-brightgreen)]()

Serviço responsável por capturar mudanças de dados (CDC) no sistema de origem e publicar eventos no Apache Kafka.

## 📋 Funcionalidades

- **Change Data Capture (CDC)** simulado com polling a cada 5 segundos
- **Publicação de eventos** no Kafka em 3 tópicos diferentes
- **Detecção automática** de tipo de evento (CREATE, UPDATE, DELETE)
- **Métricas Prometheus** para monitoramento
- **Health checks** com Spring Actuator
- **Logs estruturados** com níveis configuráveis
- **Idempotência** e garantia de entrega (acks=all)
- **Compressão** de mensagens (Snappy)

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────┐
│        Producer Service (8081)          │
├─────────────────────────────────────────┤
│                                         │
│  ┌───────────────────────────────────┐  │
│  │   ChangeDataCaptureService        │  │
│  │   (Polling CDC a cada 5s)         │  │
│  └───────────────┬───────────────────┘  │
│                  │                      │
│  ┌───────────────▼───────────────────┐  │
│  │    KafkaProducerService           │  │
│  │    (Publica no Kafka)             │  │
│  └───────────────┬───────────────────┘  │
│                  │                      │
└──────────────────┼──────────────────────┘
                   │
        ┌──────────▼──────────┐
        │   Apache Kafka      │
        │  ┌────────────────┐ │
        │  │ employee-create│ │
        │  │ employee-update│ │
        │  │ employee-delete│ │
        │  └────────────────┘ │
        └─────────────────────┘
        
```

## 🧪 Testes Automatizados

### Cobertura de Código

```

Lines:    82% ████████░░
Branches: 75% ███████░░░
Methods:  85% ████████░░
Classes: 100% ██████████

```

### Suíte de Testes (18 testes)

#### ✅ ProducerApplicationTests (1 teste)
- Validação de carregamento do contexto Spring

#### ✅ KafkaProducerServiceTest (6 testes)
- Publicação em tópico CREATE
- Publicação em tópico UPDATE
- Publicação em tópico DELETE
- Uso correto de chave de particionamento (employeeId)
- Incremento de contador de sucesso
- Incremento de contador de falha

#### ✅ ChangeDataCaptureServiceTest (8 testes)
- Processamento de colaboradores modificados
- Não processar quando não há mudanças
- Processamento e publicação de eventos
- Determinação de tipo DELETE para registros inativos
- Conversão Employee → DTO
- Incremento de contador de registros processados
- Continuidade de processamento em caso de erro
- Validação completa de campos do DTO

#### ✅ EmployeeRepositoryTest (3 testes)
- Query findModifiedAfter com filtro temporal
- Query findCreatedAfter com filtro temporal
- Ordenação correta por updated_at ascendente

### Executar Testes

```


# Executar todos os testes

mvn test

# Executar com relatório de cobertura

mvn clean test

# Ver relatório HTML

open target/site/jacoco/index.html

# Executar teste específico

mvn test -Dtest=KafkaProducerServiceTest

# Executar com logs de debug

mvn test -X

```

**Documentação completa:** Ver [TESTING.md](TESTING.md)

## ⚙️ Configuração

### Variáveis de Ambiente

| Variável                         | Descrição                 | Padrão                                       |
|----------------------------------|---------------------------|----------------------------------------------|
| `SPRING_DATASOURCE_URL`          | URL do PostgreSQL         | `jdbc:postgresql://postgres-db:5432/esocial` |
| `SPRING_DATASOURCE_USERNAME`     | Usuário do banco          | `esocial_user`                               |
| `SPRING_DATASOURCE_PASSWORD`     | Senha do banco            | `PostgresPassword123!`                       |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Brokers Kafka             | `kafka-broker-1:29092,...`                   |
| `APP_CDC_POLLING_INTERVAL`       | Intervalo de polling (ms) | `5000`                                       |
| `APP_CDC_BATCH_SIZE`             | Tamanho do lote           | `100`                                        |

### application.yml

```

spring:
kafka:
bootstrap-servers: kafka-broker-1:29092,kafka-broker-2:29092,kafka-broker-3:29092
producer:
key-serializer: org.apache.kafka.common.serialization.StringSerializer
value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
acks: all
retries: 3
properties:
enable.idempotence: true
compression.type: snappy

app:
kafka:
topics:
employee-create: employee-create
employee-update: employee-update
employee-delete: employee-delete
cdc:
polling-interval: 5000
batch-size: 100

```

## 🚀 Executar

### Localmente

```


# Com Maven

mvn spring-boot:run

# Com perfil específico

mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Com variáveis customizadas

APP_CDC_POLLING_INTERVAL=10000 mvn spring-boot:run

```

### Compilar

```


# Compilar com testes

mvn clean package

# Compilar sem testes

mvn clean package -DskipTests

# Gerar JAR executável

mvn clean package

# Resultado: target/producer-service-1.0.0-SNAPSHOT.jar

```

### Docker

```


# Build da imagem

docker build -t producer-service:latest .

# Executar container

docker run -p 8081:8081 \
-e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
-e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/esocial \
producer-service:latest

# Executar com Docker Compose (recomendado)

docker-compose up -d producer-service

# Ver logs

docker-compose logs -f producer-service

```

## 📊 Endpoints

### Actuator

| Endpoint | Descrição | Exemplo |
|----------|-----------|---------|
| `/actuator/health` | Status do serviço | `{"status":"UP"}` |
| `/actuator/prometheus` | Métricas Prometheus | `events_published_total 150.0` |
| `/actuator/info` | Informações da aplicação | `{"app":{"name":"producer-service"}}` |
| `/actuator/metrics` | Métricas detalhadas | Lista de métricas disponíveis |

### Testar Endpoints

```


# Health check

curl http://localhost:8081/actuator/health | jq

# Métricas Prometheus

curl http://localhost:8081/actuator/prometheus | grep events_published

# Métricas específicas

curl http://localhost:8081/actuator/metrics/events.published | jq

```

## 📈 Métricas Disponíveis

### Métricas de Negócio

- `events_published_total` - Total de eventos publicados
- `events_failed_total` - Total de falhas na publicação
- `cdc_records_processed_total` - Total de registros processados pelo CDC

### Métricas Técnicas

- `jvm_memory_used_bytes` - Uso de memória JVM
- `process_cpu_usage` - Uso de CPU
- `hikaricp_connections_active` - Conexões ativas no pool
- `kafka_producer_record_send_total` - Total de mensagens enviadas

### Exemplo de Query Prometheus

```


# Taxa de publicação por segundo

rate(events_published_total[1m])

# Taxa de erro

rate(events_failed_total[1m]) / rate(events_published_total[1m]) * 100

# Latência P95 de publicação

histogram_quantile(0.95, rate(kafka_producer_record_send_rate[5m]))

```

## 🔍 Monitoramento

### Logs

```


# Ver logs em tempo real

docker-compose logs -f producer-service

# Filtrar por nível

docker-compose logs producer-service | grep ERROR

# Ver últimas 100 linhas

docker-compose logs --tail=100 producer-service

```

### Health Check

```


# Verificar status

curl -s http://localhost:8081/actuator/health | jq '.status'

# Verificar detalhes

curl -s http://localhost:8081/actuator/health | jq '.'

```

## 🐛 Troubleshooting

### Problema: Não conecta ao Kafka

```


# Verificar se Kafka está acessível

docker exec producer-service ping -c 3 kafka-broker-1

# Verificar logs de conexão

docker-compose logs producer-service | grep -i kafka

# Testar conectividade

docker exec producer-service telnet kafka-broker-1 29092

```

### Problema: Não conecta ao PostgreSQL

```


# Verificar conectividade

docker exec producer-service ping -c 3 postgres-db

# Ver logs de conexão

docker-compose logs producer-service | grep -i postgres

# Testar conexão SQL

docker exec producer-service psql -h postgres-db -U esocial_user -d esocial -c "SELECT 1"

```

### Problema: CDC não captura mudanças

```


# Verificar se há registros modificados

docker exec postgres-db psql -U esocial_user -d esocial \
-c "SELECT COUNT(*) FROM source.employees WHERE updated_at > NOW() - INTERVAL '1 hour';"

# Verificar logs do CDC

docker-compose logs producer-service | grep "CDC"

# Ajustar polling interval

# Editar docker-compose.yml e reduzir APP_CDC_POLLING_INTERVAL

```

### Problema: Eventos não aparecem no Kafka

```


# Verificar tópicos criados

docker exec kafka-broker-1 kafka-topics --list --bootstrap-server localhost:9092

# Consumir mensagens do tópico

docker exec kafka-broker-1 kafka-console-consumer \
--bootstrap-server localhost:9092 \
--topic employee-create \
--from-beginning \
--max-messages 5

# Verificar métricas

curl http://localhost:8081/actuator/prometheus | grep events_published_total

```

## 📁 Estrutura do Projeto

```

producer-service/
├── src/
│   ├── main/
│   │   ├── java/com/esocial/producer/
│   │   │   ├── ProducerApplication.java          \# Main class
│   │   │   ├── config/
│   │   │   │   ├── KafkaConfig.java              \# Configuração Kafka
│   │   │   │   └── PostgresConfig.java           \# Configuração PostgreSQL
│   │   │   ├── service/
│   │   │   │   ├── ChangeDataCaptureService.java \# Lógica CDC
│   │   │   │   └── KafkaProducerService.java     \# Publicação Kafka
│   │   │   ├── model/
│   │   │   │   ├── entity/
│   │   │   │   │   └── Employee.java             \# Entity JPA
│   │   │   │   └── dto/
│   │   │   │       ├── EmployeeEventDTO.java     \# DTO de evento
│   │   │   │       └── EventType.java            \# Enum tipos
│   │   │   └── repository/
│   │   │       └── EmployeeRepository.java       \# Repository JPA
│   │   └── resources/
│   │       ├── application.yml                    \# Configuração principal
│   │       └── application-dev.yml                \# Configuração dev
│   └── test/
│       ├── java/com/esocial/producer/
│       │   ├── ProducerApplicationTests.java     \# Teste integração
│       │   ├── service/
│       │   │   ├── ChangeDataCaptureServiceTest.java
│       │   │   └── KafkaProducerServiceTest.java
│       │   └── repository/
│       │       └── EmployeeRepositoryTest.java
│       └── resources/
│           ├── application-test.yml               \# Config testes
│           └── schema.sql                         \# Schema H2
├── Dockerfile                                      \# Imagem Docker
├── pom.xml                                         \# Dependências Maven
├── README.md                                       \# Este arquivo
└── TESTING.md                                      \# Documentação de testes

```

## 🛠️ Tecnologias

- **Java 21** - Linguagem
- **Spring Boot 3.2.0** - Framework
- **Spring Kafka 3.1.0** - Integração Kafka
- **Spring Data JPA** - Persistência
- **PostgreSQL 15** - Banco de dados
- **Apache Kafka** - Message Broker
- **Micrometer** - Métricas
- **Lombok** - Redução boilerplate
- **JUnit 5** - Testes
- **Mockito** - Mocks
- **AssertJ** - Assertions
- **JaCoCo** - Cobertura de código

## 📝 Exemplo de Evento Publicado

```

{
"eventId": "a1b2c3d4-e5f6-4789-a012-3456789abcde",
"eventType": "CREATE",
"eventTimestamp": "2025-11-08T10:30:05",
"employeeId": "EMP001",
"cpf": "12345678901",
"pis": "10011223344",
"fullName": "João da Silva Santos",
"birthDate": "1985-03-15",
"admissionDate": "2020-01-10",
"terminationDate": null,
"jobTitle": "Analista de Sistemas",
"department": "TI",
"salary": 5500.00,
"status": "ACTIVE",
"createdAt": "2025-11-08T10:25:00",
"updatedAt": "2025-11-08T10:25:00",
"sourceSystem": "HR_SYSTEM",
"correlationId": "f9e8d7c6-b5a4-3210-9876-543210fedcba"
}

```

## 🔗 Links Relacionados

- [Consumer Service](../consumer-service/README.md)
- [Docker Compose Setup](../docker-compose.yml)
- [Documentação de Testes](TESTING.md)
- [Guia de Contribuição](../CONTRIBUTING.md)

## 📄 Licença

Este projeto é parte do Trabalho de Conclusão de Curso (TCC) - Todos os direitos reservados.

---

**Última atualização:** 08 de novembro de 2025  
**Versão:** 1.0.0  
**Autor:** Márcio Kuroki Gonçalves
```