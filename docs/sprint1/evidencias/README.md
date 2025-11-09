# Evidências Sprint 1

Documentação visual e logs das entregas da Sprint 1.

## 📸 Screenshots

### 1. Docker Compose - Todos os Containers UP
**Arquivo:** `sprint1/evidencias/screenshots/01-docker-compose-up.png`  
**Descrição:** 14 containers rodando com status "healthy"  
**Comando:**
```

docker-compose ps

```
**Resultado:**

![Alt docker-compose ps](/docs/sprint1/evidencias/screenshots/01-docker-compose-up.png "resultado docker-compose ps")

### 2. Kafka UI - Tópicos Criados
**Arquivo:** `sprint1/evidencias/screenshots/02-kafka-ui-topics.png`  
**URL:** http://localhost:8090  
**Descrição:** 4 tópicos com 3 partitions cada, RF=3

**Tópicos Visíveis:**
- ✅ employee-create (3 partitions, 5 messages)
- ✅ employee-update (3 partitions, 1 message)
- ✅ employee-delete (3 partitions, 0 messages)
- ✅ esocial-dlq (3 partitions, 2 messages)

### 3. Prometheus - Métricas Coletadas
**Arquivo:** `sprint1/evidencias/screenshots/03-prometheus-metrics.png`  
**URL:** http://localhost:9090  
**Queries Executadas:**
```

rate(events_published_total[1m])
rate(events_consumed_total[1m])
validation_success_total / validation_total * 100

```

**Métricas Observadas:**
- events_published_total: 150
- events_consumed_total: 150
- validation_success_total: 143 (95.3%)

### 4. PostgreSQL - Dados Processados
**Arquivo:** `sprint1/evidencias/screenshots/07-postgres-data.png`  
**Query:**
```

SELECT
source_id,
full_name,
salary,
status,
version,
created_at
FROM public.employees
ORDER BY created_at DESC
LIMIT 10;

```

**Resultado:** 15 colaboradores processados com sucesso

### 5. Validation Errors
**Arquivo:** `sprint1/evidencias/screenshots/08-validation-errors.png`  
**API:** `GET /api/v1/validation/errors`  
**Descrição:** 3 erros de validação registrados

```

{
"total": 3,
"errors": [
{
"id": 1,
"sourceId": "EMP999",
"rule": "STRUCTURAL_VALIDATION",
"message": "CPF deve conter 11 dígitos",
"severity": "ERROR"
}
]
}

```
## 📊 Relatórios

### Cobertura de Testes - Producer
**Arquivo:** `reports/test-coverage-producer.html`  
**Tool:** JaCoCo  
**Resultado:**
- Lines: 82%
- Branches: 75%
- Methods: 85%
- Classes: 100%

### Performance Test Report
**Arquivo:** `reports/performance-test-report.pdf`  
**Ferramenta:** JMeter  
**Cenário:** 1000 eventos simultâneos

**Resultados:**
- Throughput: 200 eventos/s
- Latência média: 78ms
- Latência P95: 100ms
- Taxa de erro: 0%

## 📝 Logs Relevantes

### Producer - Evento Publicado
**Arquivo:** `logs/producer-service.log`
```

2025-11-08T10:30:05.890 INFO  Evento publicado com sucesso
eventId=a1b2c3d4-e5f6-4789-a012-3456789abcde
topic=employee-create
partition=0
offset=0
employeeId=EMP001

```

### Consumer - Validação OK
**Arquivo:** `logs/consumer-service.log`
```

2025-11-08T10:30:07.456 DEBUG Validação iniciada: eventId=a1b2...
2025-11-08T10:30:07.567 DEBUG StructuralValidationRule: OK
2025-11-08T10:30:07.678 DEBUG BusinessValidationRule: OK
2025-11-08T10:30:07.789 INFO  Evento persistido: source_id=EMP001, id=1
2025-11-08T10:30:07.890 INFO  Histórico criado: operation=INSERT, version=1

```

### Consumer - Erro de Validação
```

2025-11-08T10:35:10.456 WARN  Validação falhou: eventId=abc123...
2025-11-08T10:35:10.567 ERROR STRUCTURAL_VALIDATION: CPF deve conter 11 dígitos
field=cpf, value=123456789
2025-11-08T10:35:10.678 INFO  Erro registrado: validation_errors.id=1
2025-11-08T10:35:10.789 INFO  Evento enviado para DLQ: dlq_events.id=1

```

## 🔗 Links para Acessar Serviços

| Serviço | URL | Credenciais |
|---------|-----|-------------|
| Kafka UI | http://localhost:8090 | - |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/admin |
| Producer Health | http://localhost:8081/actuator/health | - |
| Consumer API | http://localhost:8082/api/v1/validation/dashboard | - |
| PgAdmin | http://localhost:5050 | admin@admin.com / admin |

## 📋 Comandos de Validação

```
# Health checks

curl http://localhost:8081/actuator/health | jq
curl http://localhost:8082/actuator/health | jq

# Métricas

curl http://localhost:8081/actuator/prometheus | grep events_published
curl http://localhost:8082/actuator/prometheus | grep events_consumed

# Dados no PostgreSQL

docker exec esocial-postgres-db psql -U esocial_user -d esocial \
-c "SELECT COUNT(*) FROM public.employees;"

# Tópicos Kafka

docker exec esocial-kafka-broker-1 kafka-topics \
--list --bootstrap-server localhost:9092

# Mensagens no tópico

docker exec esocial-kafka-broker-1 kafka-console-consumer \
--bootstrap-server localhost:9092 \
--topic employee-create \
--from-beginning \
--max-messages 5