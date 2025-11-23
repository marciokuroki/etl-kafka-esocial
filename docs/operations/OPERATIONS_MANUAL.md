# Manual de Operação e Troubleshooting

**Versão:** 1.0  
**Data:** 2025-11-22  
**Projeto:** Pipeline ETL eSocial  
**Público-alvo:** Equipe de Operações e Sustentação

---

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Como Iniciar o Sistema](#como-iniciar-o-sistema)
3. [Verificação de Saúde](#verificação-de-saúde)
4. [Monitoramento](#monitoramento)
5. [Troubleshooting](#troubleshooting)
6. [Manutenção](#manutenção)
7. [FAQ - Perguntas Frequentes](#faq---perguntas-frequentes)
8. [Contatos de Emergência](#contatos-de-emergência)

---

## Visão Geral

### Arquitetura Resumida

```

Sistema RH (Origem)
↓ CDC (5 segundos)
Producer Service (8081)
↓ Kafka Protocol
Apache Kafka Cluster (3 brokers)
↓ Consumer Group
Consumer Service (8082)
↓ Validação + Persistência
PostgreSQL (Destino + Audit)

```

### Componentes Críticos

| Componente | Porta | Status Esperado | Impacto se Falhar |
|------------|-------|-----------------|-------------------|
| **Producer Service** | 8081 | UP | 🔴 CRÍTICO - Pipeline para |
| **Consumer Service** | 8082 | UP | 🔴 CRÍTICO - Dados não persistem |
| **Kafka Broker 1** | 9092 | RUNNING | 🟡 ALTO - Redundância OK (RF=3) |
| **Kafka Broker 2** | 9093 | RUNNING | 🟡 ALTO - Redundância OK (RF=3) |
| **Kafka Broker 3** | 9094 | RUNNING | 🟡 ALTO - Redundância OK (RF=3) |
| **PostgreSQL** | 5432 | RUNNING | 🔴 CRÍTICO - Perda de dados |
| **Zookeeper** | 2181 | RUNNING | 🔴 CRÍTICO - Kafka para |
| **Prometheus** | 9090 | RUNNING | 🟢 BAIXO - Perde métricas |
| **Grafana** | 3000 | RUNNING | 🟢 BAIXO - Perde dashboards |

### SLA do Sistema

| Métrica | Target | Limite Crítico |
|---------|--------|----------------|
| **Uptime** | 99.7% | < 99% |
| **Throughput** | 1.200 evt/s | < 500 evt/s |
| **Latência P95** | 85ms | > 200ms |
| **Taxa de Erro** | 8% | > 15% |
| **Consumer Lag** | < 100 eventos | > 1.000 eventos |

---

## Como Iniciar o Sistema

### Pré-requisitos

✅ Docker 24.0+ e Docker Compose 2.20+ instalados  
✅ 16GB RAM disponível  
✅ 20GB de espaço em disco  
✅ Portas liberadas: 2181, 3000, 5432, 8081, 8082, 8090, 9090-9094

### Procedimento de Inicialização

#### 1. Clonar Repositório (primeira vez)

```

git clone https://github.com/marciokuroki/etl-kafka-esocial.git
cd etl-kafka-esocial
git checkout sprint3  \# Branch estável

```

#### 2. Iniciar Infraestrutura Completa

```


# Iniciar todos os 14 containers

docker-compose up -d

# Saída esperada:

# Creating esocial-zookeeper        ... done

# Creating esocial-kafka-broker-1   ... done

# Creating esocial-kafka-broker-2   ... done

# Creating esocial-kafka-broker-3   ... done

# Creating esocial-postgres-db      ... done

# Creating esocial-producer-service ... done

# Creating esocial-consumer-service ... done

# Creating esocial-prometheus       ... done

# Creating esocial-alertmanager     ... done

# Creating esocial-grafana          ... done

# Creating esocial-kafka-ui         ... done

# Creating esocial-pgadmin          ... done

```

#### 3. Aguardar Inicialização (2-3 minutos)

```


# Monitorar status dos containers

watch -n 2 docker-compose ps

# Aguardar até todos ficarem "healthy" ou "Up"

# Producer e Consumer levam ~60s para inicializar

```

#### 4. Verificar Logs

```


# Logs dos serviços críticos

docker-compose logs -f producer-service consumer-service

# Buscar por:

# ✅ "Started ProducerServiceApplication in X seconds"

# ✅ "Started ConsumerServiceApplication in X seconds"

```

### Ordem de Inicialização (Automática)

Docker Compose já gerencia a ordem via `depends_on`:

1. ✅ **Zookeeper** (primeiro)
2. ✅ **Kafka Brokers** (após Zookeeper)
3. ✅ **PostgreSQL** (paralelo)
4. ✅ **Producer** (após Kafka + PostgreSQL)
5. ✅ **Consumer** (após Kafka + PostgreSQL)
6. ✅ **Observabilidade** (Prometheus, Grafana, etc)

---

## Verificação de Saúde

### Checklist Completo de Health Checks

Execute os comandos abaixo para validar o sistema:

#### ✅ 1. Health Check - Producer Service

```

curl http://localhost:8081/actuator/health | jq

# Resposta esperada:

{
"status": "UP",
"components": {
"diskSpace": { "status": "UP" },
"db": { "status": "UP" },
"kafka": { "status": "UP" },
"ping": { "status": "UP" }
}
}

```

**❌ Se falhar:**
- Verificar logs: `docker-compose logs producer-service`
- Verificar conectividade PostgreSQL: `docker exec esocial-producer-service nc -zv esocial-postgres-db 5432`
- Verificar conectividade Kafka: `docker exec esocial-producer-service nc -zv esocial-kafka-broker-1 9092`

---

#### ✅ 2. Health Check - Consumer Service

```

curl http://localhost:8082/actuator/health | jq

# Resposta esperada:

{
"status": "UP",
"components": {
"diskSpace": { "status": "UP" },
"db": { "status": "UP" },
"kafka": { "status": "UP" },
"ping": { "status": "UP" }
}
}

```

**❌ Se falhar:**
- Verificar logs: `docker-compose logs consumer-service`
- Verificar consumer group: `docker exec esocial-kafka-broker-1 kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group esocial-consumer-group`

---

#### ✅ 3. Health Check - Kafka Cluster

```


# Verificar brokers ativos

docker exec esocial-kafka-broker-1 kafka-broker-api-versions \
--bootstrap-server localhost:9092

# Saída esperada:

# esocial-kafka-broker-1:9092 (id: 1 rack: null) -> (

# ApiVersion(apiKey=..., minVersion=..., maxVersion=...)

# )

```

```


# Verificar tópicos

docker exec esocial-kafka-broker-1 kafka-topics \
--bootstrap-server localhost:9092 --list

# Saída esperada:

# employee-create

# employee-update

# employee-delete

# dlq-events

```

**❌ Se falhar:**
- Verificar Zookeeper: `echo stat | nc localhost 2181`
- Verificar logs Kafka: `docker-compose logs kafka-broker-1 kafka-broker-2 kafka-broker-3`

---

#### ✅ 4. Health Check - PostgreSQL

```


# Conectar no banco

docker exec esocial-postgres-db psql -U esocial_user -d esocial -c "SELECT 1"

# Saída esperada:

# ?column?

# ----------

# 1

# (1 row)

```

```


# Verificar schemas

docker exec esocial-postgres-db psql -U esocial_user -d esocial -c "\dn"

# Saída esperada:

# Name   |  Owner

# ----------+--------------

# audit    | esocial_user

# public   | esocial_user

# source   | esocial_user

```

**❌ Se falhar:**
- Verificar container: `docker inspect esocial-postgres-db`
- Verificar logs: `docker-compose logs postgres-db`

---

#### ✅ 5. Health Check - Prometheus

```

curl http://localhost:9090/-/healthy

# Resposta esperada: "Prometheus is Healthy."

```

```


# Verificar targets

curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'

# Saída esperada:

# { "job": "producer-service", "health": "up" }

# { "job": "consumer-service", "health": "up" }

```

**❌ Se falhar:**
- Verificar arquivo de configuração: `docker exec esocial-prometheus cat /etc/prometheus/prometheus.yml`

---

#### ✅ 6. Validação de Conectividade - Matriz

| De | Para | Porta | Comando | Status Esperado |
|----|------|-------|---------|-----------------|
| Producer | PostgreSQL | 5432 | `docker exec esocial-producer-service nc -zv esocial-postgres-db 5432` | succeeded |
| Producer | Kafka Broker 1 | 9092 | `docker exec esocial-producer-service nc -zv esocial-kafka-broker-1 9092` | succeeded |
| Consumer | PostgreSQL | 5432 | `docker exec esocial-consumer-service nc -zv esocial-postgres-db 5432` | succeeded |
| Consumer | Kafka Broker 1 | 9092 | `docker exec esocial-consumer-service nc -zv esocial-kafka-broker-1 9092` | succeeded |
| Kafka | Zookeeper | 2181 | `docker exec esocial-kafka-broker-1 nc -zv esocial-zookeeper 2181` | succeeded |
| Prometheus | Producer | 8081 | `docker exec esocial-prometheus nc -zv esocial-producer-service 8081` | succeeded |
| Prometheus | Consumer | 8082 | `docker exec esocial-prometheus nc -zv esocial-consumer-service 8082` | succeeded |

---

### Script Automatizado de Health Check

Crie o arquivo `scripts/health-check.sh`:

```

\#!/bin/bash

# Script de Health Check Automatizado

echo "========================================="
echo "Pipeline ETL eSocial - Health Check"
echo "========================================="
echo ""

# Função para verificar status

check_service() {
local service=\$1
local url=\$2

    echo -n "Verificando $service... "
    
    if curl -s -f "$url" > /dev/null; then
        echo "✅ UP"
        return 0
    else
        echo "❌ DOWN"
        return 1
    fi
    }

# Producer

check_service "Producer Service" "http://localhost:8081/actuator/health"

# Consumer

check_service "Consumer Service" "http://localhost:8082/actuator/health"

# Prometheus

check_service "Prometheus" "http://localhost:9090/-/healthy"

# Grafana

check_service "Grafana" "http://localhost:3000/api/health"

# Kafka UI

check_service "Kafka UI" "http://localhost:8090"

# PostgreSQL

echo -n "Verificando PostgreSQL... "
if docker exec esocial-postgres-db psql -U esocial_user -d esocial -c "SELECT 1" > /dev/null 2>\&1; then
echo "✅ UP"
else
echo "❌ DOWN"
fi

# Kafka Brokers

for i in 1 2 3; do
echo -n "Verificando Kafka Broker $i... "
    if docker exec esocial-kafka-broker-$i kafka-broker-api-versions --bootstrap-server localhost:909\$((i+1)) > /dev/null 2>\&1; then
echo "✅ UP"
else
echo "❌ DOWN"
fi
done

echo ""
echo "========================================="
echo "Health Check Concluído"
echo "========================================="

```

**Uso:**
```

chmod +x scripts/health-check.sh
./scripts/health-check.sh

```

---

## Monitoramento

### Como Acessar as Ferramentas de Monitoramento

#### 1. Grafana (Dashboards)

**URL:** http://localhost:3000  
**Login:** `admin`  
**Senha:** `admin` (será solicitada mudança no primeiro acesso)

**Dashboards Disponíveis:**

| Dashboard | Descrição | URL |
|-----------|-----------|-----|
| **Overview Geral** | Visão consolidada do sistema | Grafana → Dashboards → Overview |
| **Producer Metrics** | CDC + Kafka Producer | Grafana → Dashboards → Producer |
| **Consumer Metrics** | Validação + Persistência | Grafana → Dashboards → Consumer |
| **Kafka Cluster Health** | Brokers + Topics + Lag | Grafana → Dashboards → Kafka |
| **Validation Dashboard** | Erros + DLQ + Regras | Grafana → Dashboards → Validation |

**Screenshot Exemplo:**

![Grafana Overview Dashboard](../assets/screenshots/grafana-overview.png)

---

#### 2. Prometheus (Métricas)

**URL:** http://localhost:9090  
**Autenticação:** Não requerida

**Principais Queries:**
# Throughput de eventos publicados (Producer)
```
rate(events_published_total[5m])
```
# Taxa de erro (Consumer)
```
rate(validation_failure_total[5m]) / rate(events_consumed_total[5m]) * 100
```
# Latência P95 do Consumer
```
histogram_quantile(0.95, rate(validation_duration_seconds_bucket[5m]))
```
# Eventos pendentes na DLQ
```
dlq_events_pending
```
# Consumer Lag
```
kafka_consumergroup_lag{group="esocial-consumer-group"}

```

---

#### 3. Alertmanager (Alertas)

**URL:** http://localhost:9093  
**Autenticação:** Não requerida

**Como visualizar alertas ativos:**
1. Acesse http://localhost:9093/#/alerts
2. Visualize alertas em:
   - 🔴 **Firing** - Disparado (ação necessária)
   - 🟡 **Pending** - Pendente (aguardando confirmação)
   - 🟢 **Resolved** - Resolvido

---

#### 4. Kafka UI (Provectus)

**URL:** http://localhost:8090  
**Autenticação:** Não requerida

**Funcionalidades:**
- Visualizar tópicos e partições
- Monitorar consumer groups e lag
- Inspecionar mensagens (últimas 100)
- Visualizar configuração de brokers

---

### Principais Métricas e Limites Aceitáveis

#### Producer Service Metrics
```
| Métrica | Nome Prometheus | Valor Normal | Limite Alerta | Ação |
|---------|----------------|--------------|---------------|------|
| **Throughput** | `rate(events_published_total[5m])` | 800-1.500 evt/s | < 500 evt/s | Investigar CDC |
| **Latência CDC** | `histogram_quantile(0.95, cdc_polling_duration_seconds_bucket)` | 30-80ms | > 10s | Otimizar query/DB |
| **Erros Kafka** | `kafka_publish_errors_total` | 0-5 erros/min | > 50 erros/min | Verificar Kafka |
| **Heap JVM** | `jvm_memory_used_bytes{area="heap"}` | < 1GB | > 1.5GB | Memory leak |
```

**Como visualizar:**
```
curl http://localhost:8081/actuator/prometheus | grep events_published
```

---

#### Consumer Service Metrics
```
| Métrica | Nome Prometheus | Valor Normal | Limite Alerta | Ação |
|---------|----------------|--------------|---------------|------|
| **Throughput** | `rate(events_consumed_total[5m])` | 800-1.500 evt/s | < 500 evt/s | Escalar consumer |
| **Taxa de Sucesso** | `validation_success_total / events_consumed_total * 100` | > 85% | < 75% | Revisar validações |
| **Taxa de Erro** | `validation_failure_total / events_consumed_total * 100` | 8-12% | > 20% | Dados incorretos |
| **DLQ Acumulada** | `dlq_events_pending` | < 100 | > 1.000 | Reprocessar DLQ |
| **Consumer Lag** | `kafka_consumergroup_lag` | < 100 eventos | > 1.000 eventos | Escalar pods |
| **Latência Validação** | `histogram_quantile(0.95, validation_duration_seconds_bucket)` | 50-120ms | > 500ms | Otimizar regras |
| **Latência Persistência** | `histogram_quantile(0.95, persistence_duration_seconds_bucket)` | 5-15ms | > 100ms | Otimizar DB |
```

**Como visualizar:**

```
curl http://localhost:8082/actuator/prometheus | grep validation
```

---

#### Kafka Cluster Metrics
```
| Métrica | Como Verificar | Valor Normal | Limite Alerta |
|---------|----------------|--------------|---------------|
| **Brokers Ativos** | Kafka UI → Brokers | 3 | < 2 |
| **Under-Replicated Partitions** | Kafka UI → Topics | 0 | > 0 |
| **Consumer Lag** | Kafka UI → Consumer Groups | < 100 | > 1.000 |
| **Messages In** | Kafka UI → Topics | 800-1.500/s | < 500/s |
| **Disk Usage** | Kafka UI → Brokers → Metrics | < 70% | > 85% |
```
---

#### PostgreSQL Metrics
```
| Métrica | Como Verificar | Valor Normal | Limite Alerta |
|---------|----------------|--------------|---------------|
| **Connections** | `SELECT count(*) FROM pg_stat_activity;` | 10-50 | > 100 |
| **Disk Usage** | `SELECT pg_database_size('esocial');` | < 10GB | > 50GB |
| **Lock Waits** | `SELECT count(*) FROM pg_locks WHERE NOT granted;` | 0-5 | > 20 |
| **Replication Lag** | (se aplicável) | 0 | > 1s |
```

# Verificar conexões ativas
```
docker exec esocial-postgres-db psql -U esocial_user -d esocial -c \
"SELECT count(*) as connections FROM pg_stat_activity;"

```

---

## Troubleshooting

### Problema 1: Consumer Lag Alto (> 1.000 eventos)

**Sintomas:**
- Kafka UI mostra lag crescente no consumer group
- Dashboard Grafana: gráfico de lag subindo
- Alerta: `ConsumerLagHigh` disparado

**Diagnóstico:**
# 1. Verificar lag atual
```
docker exec esocial-kafka-broker-1 kafka-consumer-groups \
--bootstrap-server localhost:9092 \
--describe --group esocial-consumer-group

# Saída:

# TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG

# employee-create    0          1000            2500         1500  ← LAG ALTO!

```

**Causas Possíveis:**

1. **Consumer lento** (validações complexas)
# Verificar latência de validação
```
curl http://localhost:8082/actuator/prometheus | grep validation_duration
```

2. **Throughput alto repentino**

# Verificar rate de mensagens
```
curl http://localhost:8081/actuator/prometheus | grep events_published
```

3. **Consumer pausado/travado**
# Verificar logs do consumer
```
docker-compose logs --tail=100 consumer-service | grep ERROR

```

**Soluções:**

✅ **Solução 1: Escalar Consumer (Kubernetes)**
# Aumentar réplicas (production)
```
kubectl scale deployment consumer-service --replicas=3
```

✅ **Solução 2: Aumentar Partições**
# Aumentar de 3 para 6 partições
```
docker exec esocial-kafka-broker-1 kafka-topics \
--bootstrap-server localhost:9092 \
--alter --topic employee-create --partitions 6

```

✅ **Solução 3: Otimizar Validações**
- Revisar regras de validação lentas
- Adicionar cache para lookups frequentes
- Implementar batch processing

✅ **Solução 4: Restart Consumer (temporário)**
```
docker-compose restart consumer-service
```

---

### Problema 2: Taxa de Erro Elevada (> 15%)

**Sintomas:**
- Dashboard mostra validation_failure_total alto
- Alerta: `HighErrorRate` disparado
- DLQ acumulando eventos

**Diagnóstico:**

# 1. Listar erros recentes
```
curl http://localhost:8082/api/v1/validation/errors | jq '.[] | {rule: .ruleName, field: .fieldName, count: .count}' | head -20
```
# Saída exemplo:

# {

# "rule": "INVALID_CPF_FORMAT",

# "field": "cpf",

# "count": 245

# }

```

```


# 2. Verificar distribuição de erros

curl http://localhost:8082/api/v1/validation/dashboard | jq '.errorsByRule'

# Saída exemplo:

# {

# "INVALID_CPF_FORMAT": 245,

# "MINIMUM_AGE_VIOLATION": 89,

# "FUTURE_DATE": 34

# }

```

**Causas Possíveis:**

1. **Dados incorretos no sistema origem**
   - Verificar qualidade dos dados em `source.employees`
   - Executar data quality checks

2. **Regras de validação muito restritivas**
   - Revisar regras com alta taxa de falha
   - Considerar mudar severidade ERROR → WARNING

3. **Bug em regra de validação**
   - Revisar código da regra específica
   - Executar testes unitários

**Soluções:**

✅ **Solução 1: Corrigir Dados na Origem**
```
-- Exemplo: Corrigir CPFs inválidos
UPDATE source.employees
SET cpf = lpad(cpf, 11, '0')  -- Adicionar zeros à esquerda
WHERE length(cpf) < 11;

```

✅ **Solução 2: Ajustar Severidade da Regra**
```
// Mudar de ERROR para WARNING (não bloqueia)
@Component
public class MinimumSalaryValidationRule extends AbstractValidationRule {
    public MinimumSalaryValidationRule() {
    super("BELOW_MINIMUM_SALARY", ValidationSeverity.WARNING, 13);
    //                                                ^^^^^^^^
    }
}

```

✅ **Solução 3: Desabilitar Regra Temporariamente**
```
# application.yml (consumer-service)

validation:
rules:
disabled:
- MinimumSalaryValidationRule  \# Desabilita temporariamente

```

---

### Problema 3: Eventos Acumulados na DLQ

**Sintomas:**
- Dashboard mostra `dlq_events_pending` > 100
- Alerta: `DLQAccumulating` disparado
- Eventos não sendo reprocessados

**Diagnóstico:**
# 1. Listar eventos na DLQ

curl http://localhost:8082/api/v1/validation/dlq | jq '.[] | {id, eventId, errorMessage, retryCount}'

# Saída exemplo:

# {

# "id": 123,

# "eventId": "evt-456",

# "errorMessage": "INVALID_CPF_FORMAT: CPF '123' deve ter 11 dígitos",

# "retryCount": 0

# }

```




# 2. Verificar status dos eventos DLQ
```
curl http://localhost:8082/api/v1/validation/dlq | jq 'group_by(.status) | map({status: ..status, count: length})'

# Saída exemplo:

# { "status": "PENDING", "count": 87 },

# { "status": "FAILED", "count": 13 }

# ]

```

**Causas Possíveis:**

1. **Dados ainda incorretos** (erro persistente)
2. **Max retries atingido** (retryCount >= 3)
3. **Erro sistemático** (bug na validação)

**Soluções:**

✅ **Solução 1: Reprocessar Eventos Manualmente**
# Reprocessar evento específico
```
curl -X POST http://localhost:8082/api/v1/validation/dlq/123/retry
```
# Resposta:

# {

# "success": true,

# "message": "Event reprocessed successfully"

# }

✅ **Solução 2: Reprocessar Batch (SQL direto)**
```
-- Conectar no PostgreSQL
docker exec -it esocial-postgres-db psql -U esocial_user -d esocial

-- Marcar eventos PENDING para retry (reset retryCount)
UPDATE dlq_events
SET retry_count = 0, status = 'PENDING', updated_at = NOW()
WHERE status = 'FAILED' AND retry_count < max_retries;
```

✅ **Solução 3: Corrigir Dados e Reprocessar**

# 1. Corrigir dados no sistema origem
```
docker exec -it esocial-postgres-db psql -U esocial_user -d esocial -c \
"UPDATE source.employees SET cpf = '12345678901' WHERE employee_id = 'EMP100';"
```
# 2. Aguardar CDC processar (5 segundos)

# 3. Limpar DLQ antiga (opcional)
```
curl -X DELETE http://localhost:8082/api/v1/validation/dlq/123
```

---

### Problema 4: Performance Degradada (Latência Alta)

**Sintomas:**
- Latência P95 > 200ms
- Dashboard mostra gráfico de latência subindo
- Usuários reportam lentidão

**Diagnóstico:**

# 1. Verificar latência por componente
```
curl http://localhost:8082/actuator/prometheus | grep duration_seconds
```
# Métricas importantes:

# - validation_duration_seconds  ← Tempo de validação

# - persistence_duration_seconds ← Tempo de persistência

# 2. Verificar uso de CPU/Memória
```
docker stats esocial-consumer-service --no-stream
```
# Saída:

# CONTAINER ID   CPU %     MEM USAGE / LIMIT   MEM %

# abc123...      85.50%    1.8GiB / 2GiB       90.00%  ← ALTO!

```
# 3. Verificar slow queries no PostgreSQL
```
docker exec esocial-postgres-db psql -U esocial_user -d esocial -c \
"SELECT query, mean_exec_time, calls FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"
```

**Causas Possíveis:**

1. **Heap JVM insuficiente** (Consumer)
2. **Validações complexas** (regex, loops)
3. **Queries PostgreSQL lentas** (falta índice)
4. **Consumer lag causando backpressure**

**Soluções:**

✅ **Solução 1: Aumentar Heap JVM**
# docker-compose.yml
```
consumer-service:
environment:
- JAVA_OPTS=-Xmx2g -Xms1g  \# Aumentar de 1GB para 2GB

docker-compose up -d consumer-service  \# Restart
```

✅ **Solução 2: Otimizar Query Lenta**
```
-- Adicionar índice faltante
CREATE INDEX idx_employees_updated_at ON source.employees(updated_at);
CREATE INDEX idx_employees_source_id ON public.employees(source_id);

```

✅ **Solução 3: Tune Connection Pool**
# application.yml (consumer-service)
```
spring:
datasource:
hikari:
maximum-pool-size: 20  \# Aumentar de 10 para 20
minimum-idle: 5
connection-timeout: 30000

```

✅ **Solução 4: Desabilitar Validações Não-Críticas**

# Temporariamente desabilitar regras lentas
```
validation:
rules:
disabled:
- StatusTransitionValidationRule  \# Se muito lenta
```

---

### Problema 5: Producer Service Down

**Sintomas:**
- Health check retorna 503/DOWN
- Logs mostram erros de conexão
- Eventos não sendo publicados no Kafka

**Diagnóstico:**
# 1. Verificar status do container
```
docker-compose ps producer-service
```
# 2. Verificar logs
```
docker-compose logs --tail=100 producer-service
```
# Buscar por:

# - "Connection refused" (Kafka/PostgreSQL down)

# - "OutOfMemoryError" (heap insuficiente)

# - "SQLException" (problema no banco)

**Causas Possíveis:**

1. **PostgreSQL inacessível**
2. **Kafka inacessível**
3. **Out of Memory (OOM)**
4. **Deadlock/travamento**

**Soluções:**

✅ **Solução 1: Restart Rápido**
```
docker-compose restart producer-service
```
# Verificar logs após restart
```
docker-compose logs -f producer-service
```

✅ **Solução 2: Verificar Dependências**
# PostgreSQL está UP?
```
docker exec esocial-postgres-db psql -U esocial_user -d esocial -c "SELECT 1"
```
# Kafka está UP?
```
docker exec esocial-kafka-broker-1 kafka-broker-api-versions --bootstrap-server localhost:9092
```

✅ **Solução 3: Rebuild do Container**
# Se problema persistir, rebuild
```
docker-compose build producer-service
docker-compose up -d producer-service
```

---

### Problema 6: Kafka Broker Down

**Sintomas:**
- Kafka UI mostra apenas 1-2 brokers
- Alerta: `KafkaBrokerDown` disparado
- Producer/Consumer com erros de conexão

**Diagnóstico:**
# 1. Verificar quais brokers estão UP
```
docker-compose ps | grep kafka-broker
```
# 2. Verificar logs do broker down
```
docker-compose logs kafka-broker-2  \# Exemplo

```

**Causas Possíveis:**

1. **Zookeeper down** (coordenação perdida)
2. **Disco cheio** (logs Kafka)
3. **Out of Memory**
4. **Network issue**

**Soluções:**

✅ **Solução 1: Restart Broker**
```
docker-compose restart kafka-broker-2
```

✅ **Solução 2: Verificar Zookeeper**
# Zookeeper está respondendo?
```
echo stat | nc localhost 2181
```
# Saída esperada:
```
Zookeeper version: 3.8.0
```
# Clients: ...

```

✅ **Solução 3: Limpar Logs Antigos**
# Verificar uso de disco
```
docker exec esocial-kafka-broker-2 df -h
```
# Limpar logs antigos (se disco cheio)
```
docker exec esocial-kafka-broker-2 kafka-log-dirs \
--describe --bootstrap-server localhost:9093
```

---

## Manutenção

### Procedimento de Backup

#### 1. Backup PostgreSQL (Diário)

```
\#!/bin/bash

# scripts/backup-postgresql.sh

BACKUP_DIR="/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/esocial_backup_\$DATE.sql"
```
# Criar diretório se não existir
```
mkdir -p \$BACKUP_DIR
```
# Backup completo
```
docker exec esocial-postgres-db pg_dump -U esocial_user esocial > \$BACKUP_FILE
```
# Comprimir
```
gzip \$BACKUP_FILE
```
# Manter apenas últimos 7 dias
```
find \$BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup concluído: \$BACKUP_FILE.gz"
```

**Agendar via cron:**
# Executar todo dia às 2h da manhã
```
0 2 * * * /path/to/scripts/backup-postgresql.sh
```

---

#### 2. Backup Grafana Dashboards

```
\#!/bin/bash

# scripts/backup-grafana.sh

BACKUP_DIR="/backups/grafana"
DATE=\$(date +%Y%m%d_%H%M%S)

mkdir -p \$BACKUP_DIR
```
# Backup do banco de dados do Grafana
```
docker cp esocial-grafana:/var/lib/grafana/grafana.db \
$BACKUP_DIR/grafana_$DATE.db
```
# Comprimir
```
tar -czf $BACKUP_DIR/grafana_$DATE.tar.gz -C $BACKUP_DIR grafana_$DATE.db
```
# Limpar arquivo temp
```
rm $BACKUP_DIR/grafana_$DATE.db

echo "Backup Grafana concluído: grafana_\$DATE.tar.gz"
```

---

#### 3. Backup Configurações (Arquivos YAML)
```
\#!/bin/bash

# scripts/backup-configs.sh

BACKUP_DIR="/backups/configs"
DATE=\$(date +%Y%m%d_%H%M%S)

mkdir -p \$BACKUP_DIR
```
# Backup docker-compose e configs
```
tar -czf $BACKUP_DIR/configs_$DATE.tar.gz \
docker-compose.yml \
prometheus/prometheus.yml \
prometheus/alert-rules.yml \
alertmanager/alertmanager.yml

echo "Backup configs concluído: configs_\$DATE.tar.gz"
```

---

### Procedimento de Restore

#### Restore PostgreSQL
```
\#!/bin/bash

# scripts/restore-postgresql.sh

BACKUP_FILE=\$1

if [ -z "\$BACKUP_FILE" ]; then
echo "Uso: ./restore-postgresql.sh <arquivo_backup.sql.gz>"
exit 1
fi
```
# Descomprimir
```
gunzip -c \$BACKUP_FILE > /tmp/restore.sql
```
# Restore
```
docker exec -i esocial-postgres-db psql -U esocial_user esocial < /tmp/restore.sql
```
# Limpar
```
rm /tmp/restore.sql

echo "Restore concluído!"
```

---

### Limpeza de Dados Antigos

#### 1. Limpar DLQ Events Antigos (> 30 dias)

```
-- Executar mensalmente
DELETE FROM dlq_events
WHERE created_at < NOW() - INTERVAL '30 days'
AND status = 'REPROCESSED';
```

#### 2. Limpar Validation Errors Antigos (> 90 dias)

```
DELETE FROM validation_errors
WHERE created_at < NOW() - INTERVAL '90 days';
```

#### 3. Particionar Audit History (Performance)

```
-- Criar partições mensais
CREATE TABLE audit.employees_history_202511 PARTITION OF audit.employees_history
FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE audit.employees_history_202512 PARTITION OF audit.employees_history
FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

```

---

### Atualização de Versão

#### Procedimento Seguro de Deploy
```
\#!/bin/bash

# scripts/deploy-new-version.sh

VERSION=\$1

if [ -z "\$VERSION" ]; then
echo "Uso: ./deploy-new-version.sh <versao>"
exit 1
fi

echo "Iniciando deploy da versão \$VERSION..."
```
# 1. Backup antes de atualizar
```
./scripts/backup-postgresql.sh
./scripts/backup-grafana.sh
```
# 2. Pull nova imagem
```
docker pull marciokuroki/esocial-producer:$VERSION
docker pull marciokuroki/esocial-consumer:$VERSION
```
# 3. Update docker-compose.yml
```
sed -i "s/esocial-producer:.*/esocial-producer:$VERSION/" docker-compose.yml
sed -i "s/esocial-consumer:.*/esocial-consumer:$VERSION/" docker-compose.yml
```
# 4. Deploy com downtime mínimo
```
docker-compose up -d --no-deps producer-service
sleep 30  \# Aguardar inicialização

docker-compose up -d --no-deps consumer-service
sleep 30
```
# 5. Health check
```
./scripts/health-check.sh
echo "Deploy da versão \$VERSION concluído!"
```

---

### Rotação de Logs

```
# /etc/logrotate.d/docker-esocial

/var/lib/docker/containers/*/*.log {
daily
rotate 7
compress
delaycompress
missingok
notifempty
copytruncate
}

```

---

## FAQ - Perguntas Frequentes

### Como verificar se o sistema está processando eventos?
# Verificar throughput do Producer
```
curl http://localhost:8081/actuator/prometheus | grep events_published_total
```
# Verificar throughput do Consumer
```
curl http://localhost:8082/actuator/prometheus | grep events_consumed_total
```

---

### Como inserir um colaborador de teste?

# 1. Conectar no PostgreSQL
```
docker exec -it esocial-postgres-db psql -U esocial_user -d esocial
```
# 2. Inserir
```
INSERT INTO source.employees VALUES (
'EMP999',
'12345678901',
'10011223344',
'Teste Silva',
'1990-01-01',
'2024-01-01',
NULL,
'Analista',
'TI',
5500.00,
'ACTIVE',
NOW(),
NOW()
);
```
# 3. Aguardar 5 segundos (CDC)

# 4. Verificar processamento
```
SELECT * FROM public.employees WHERE source_id = 'EMP999';
```

---

### Como limpar completamente o ambiente?

## ATENÇÃO: Isso apaga TODOS os dados!

# 1. Parar todos os containers
```
docker-compose down
```
# 2. Remover volumes (dados permanentes)
```
docker-compose down -v
```
# 3. Remover imagens
```
docker rmi \$(docker images 'esocial*' -q)
```
# 4. Iniciar do zero
```
docker-compose up -d

```

---

### Como verificar a versão dos serviços?

# Producer
```
curl http://localhost:8081/actuator/info | jq '.build.version'
```
# Consumer
```
curl http://localhost:8082/actuator/info | jq '.build.version'

```

---

### Como escalar o Consumer para processar mais eventos?

**Docker Compose (local):**

# Não suportado nativamente, apenas 1 réplica

**Kubernetes (produção):**
```
kubectl scale deployment consumer-service --replicas=3

```

---

### Como exportar dados do Grafana?

# Dashboards
```
curl -H "Authorization: Bearer <API_KEY>" \
http://localhost:3000/api/dashboards/db/overview-geral > dashboard-backup.json
```
# Datasources
```
curl -H "Authorization: Bearer <API_KEY>" \
http://localhost:3000/api/datasources > datasources-backup.json

```

---

### Como reiniciar apenas um serviço específico?

# Producer
```
docker-compose restart producer-service
```
# Consumer
```
docker-compose restart consumer-service
```
# Kafka Broker 2
```
docker-compose restart kafka-broker-2
```
# PostgreSQL
```
docker-compose restart postgres-db
```

---

## Contatos de Emergência

| Papel | Nome | Telefone | Email | Disponibilidade |
|-------|------|----------|-------|-----------------|
| **Arquiteto** | Márcio Kuroki | - | marciokuroki@gmail.com | 24x7 |

---

## Logs de Incidentes

Template para documentar incidentes:

```


### Incidente \#001 - [Título]

**Data:** YYYY-MM-DD HH:MM
**Severidade:** CRÍTICA | ALTA | MÉDIA | BAIXA
**Duração:** XX minutos
**Impacto:** Descrição do impacto aos usuários

**Sintomas:**

- Sintoma 1
- Sintoma 2

**Causa Raiz:**
Descrição da causa identificada

**Ações Tomadas:**

1. Ação 1
2. Ação 2

**Resolução:**
Como foi resolvido

**Lições Aprendidas:**

- Lição 1
- Lição 2

**Ações Preventivas:**

- [ ] Ação preventiva 1
- [ ] Ação preventiva 2

```

---

## Changelog

| Versão | Data | Autor | Mudanças |
|--------|------|-------|----------|
| 1.0 | 2025-11-22 | Márcio Kuroki | Criação inicial |

---

**Última atualização:** 2025-11-22  
**Versão do Sistema:** 1.0.0  
**Responsável:** Márcio Kuroki Gonçalves