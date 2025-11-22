# Consumer Service

Serviço responsável por consumir eventos do Kafka, validar dados e persistir no PostgreSQL.

## Funcionalidades

- Consumo de eventos Kafka (employee-create, employee-update, employee-delete)
- Validação estrutural e de negócio
- Persistência no PostgreSQL
- Versionamento com histórico (audit trail)
- Dead Letter Queue (DLQ)
- API REST para relatórios de validação
- Métricas Prometheus

## Uso do Correlation ID e Logs Estruturados

- Cada evento consumido carrega um `correlationId` do tipo UUID, extraído do header Kafka `"X-Correlation-Id"`.
- O correlation ID é atribuído ao DTO `EmployeeEventDTO` para propagação interna e rastreamento.
- Durante o processamento, o correlation ID é inserido no MDC (Mapped Diagnostic Context) para enriquecer os logs estruturados em JSON.
- Essa prática garante rastreabilidade completa dos eventos em sistemas distribuídos, facilitando debug, monitoramento e análise.
- Logs JSON estão configurados via Logback com Logstash Encoder no arquivo `src/main/resources/logback-spring.xml`.
- Mantém-se coerência no fluxo de dados entre Producer e Consumer.

## Endpoints da API

### Validação

- `GET /api/v1/validation/errors` - Lista todos os erros
- `GET /api/v1/validation/errors/recent?hours=24` - Erros recentes
- `GET /api/v1/validation/errors/severity/{severity}` - Erros por severidade
- `GET /api/v1/validation/errors/stats` - Estatísticas de erros
- `GET /api/v1/validation/dlq` - Lista eventos na DLQ
- `GET /api/v1/validation/dashboard` - Dashboard geral

### Actuator

- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Métricas Prometheus
- `GET /actuator/metrics` - Métricas detalhadas

## Executar Localmente
```
mvn spring-boot:run
```
## Compilar
```
mvn clean package
```
## Docker
```
docker build -t consumer-service:latest .
docker run -p 8082:8082 consumer-service:latest
```

## Regras de Validação

### Estruturais

- CPF obrigatório (11 dígitos)
- PIS opcional (11 dígitos se informado)
- Nome completo obrigatório
- Data de admissão obrigatória
- Salário deve ser positivo

### Negócio

- Idade mínima: 16 anos
- Data de nascimento não pode ser futura
- Data de admissão não pode ser futura
- Data de demissão deve ser posterior à admissão
- Salário mínimo: R$ 1.320,00 (warning)
