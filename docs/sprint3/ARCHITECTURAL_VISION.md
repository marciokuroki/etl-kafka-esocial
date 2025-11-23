# Visão Arquitetural - Pipeline ETL eSocial

**Versão:** 2.0  
**Data:** 2025-11-22  
**Autor:** Márcio Kuroki Gonçalves  
**Status:** Completo (Sprint 3)

---

## Sumário Executivo

O **Pipeline ETL eSocial** é uma solução enterprise de streaming de dados event-driven que automatiza a captura, validação e envio de informações trabalhistas ao portal governamental eSocial, garantindo conformidade legal, rastreabilidade completa e zero perda de dados.

### Objetivos Principais

1. **Conformidade Legal:** 100% aderência às regras do eSocial
2. **Resiliência:** Zero perda de dados com garantias ACID
3. **Observabilidade:** Monitoramento em tempo real com alertas proativos
4. **Escalabilidade:** Suportar 10.000+ eventos/segundo

---

## Arquitetura de Alto Nível

### Visão Geral
```
┌────────────────────────────────────────────────────────────┐
│ PIPELINE ETL eSOCIAL                                       │
├────────────────────────────────────────────────────────────┤
│                                                            │
│ ┌──────────┐      ┌──────────┐       ┌──────────┐          │
│ │ Sistema  │      │ Producer │       │ Apache   │          │
│ │ RH       │ ──>  │ Service  │ ────> │ Kafka    │          │
│ │ (Origem) │ CDC  │ (Java 21)│ Pub   │ (3 nodes)│          │
│ └──────────┘      └──────────┘       └─────┬────┘          │
│                                            │               │
│                                            ▼               │
│                                      ┌──────────┐          │
│                                      │ Consumer │          │
│                                      │ Service  │          │
│                                      │ (Java 21)│          │
│                                      └─────┬────┘          │
│                                            │               │
│                                            ▼               │
│                                      ┌──────────┐          │
│                                      │PostgreSQL│          │
│                                      │ (Destino)│          │
│                                      └──────────┘          │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### Princípios Arquiteturais

#### 1. Event-Driven Architecture (EDA)
- **Desacoplamento:** Serviços independentes via mensageria
- **Assíncrono:** Processamento não bloqueante
- **Resiliente:** Retry automático e DLQ

#### 2. Domain-Driven Design (DDD)
- **Bounded Contexts:** Producer (CDC) / Consumer (Validation)
- **Aggregate Roots:** Employee (entidade principal)
- **Value Objects:** CPF, PIS, Status

#### 3. Observability by Design
- **Métricas:** Prometheus + Grafana (15 métricas customizadas)
- **Logs Estruturados:** JSON + Correlation ID
- **Alertas:** Alertmanager com 15 regras

#### 4. Zero Data Loss
- **Kafka:** Replication Factor 3, Acks=all
- **PostgreSQL:** Transações ACID, Audit Trail
- **Idempotência:** Offset tracking + Deduplicação

---

## Decisões Arquiteturais Críticas

### Por Que Apache Kafka?

| Requisito | Kafka | Alternativa | Decisão |
|-----------|-------|-------------|---------|
| **Throughput** | 1M msgs/s | RabbitMQ: 50K/s | ✅ Kafka |
| **Persistência** | Log durável | RabbitMQ: Memória | ✅ Kafka |
| **Replicação** | Nativa | AWS SQS: N/A | ✅ Kafka |
| **Custo** | Open-source | AWS SQS: Pay-per-use | ✅ Kafka |

**Referência:** ADR-0001

### Por Que PostgreSQL?

| Requisito | PostgreSQL | MongoDB | Oracle |
|-----------|------------|---------|--------|
| **ACID** | ✅ Completo | ⚠️ Parcial | ✅ Completo |
| **JSONB** | ✅ Nativo | ✅ Nativo | ⚠️ Limitado |
| **Custo** | ✅ Gratuito | ✅ Gratuito | ❌ $17k/CPU |
| **Audit Trail** | ✅ Triggers | ❌ Manual | ✅ Flashback |

**Referência:** ADR-0006

### Por Que Validações em 3 Camadas?
```
Camada 1: Estrutural (formato, obrigatoriedade)
↓ Se válido
Camada 2: Negócio (idade, datas, salário)
↓ Se válido
Camada 3: eSocial (XSD, tabelas gov - futuro)
```

**Benefícios:**
- ✅ Fail-fast (rejeita rápido erros óbvios)
- ✅ Extensível (adicionar regras sem modificar código)
- ✅ Testável (cada camada isoladamente)

**Referência:** ADR-0007

---

## Fluxo de Dados End-to-End

### 1. Captura de Mudanças (CDC)
```
-- Producer Service executa a cada 5 segundos
SELECT * FROM source.employees
WHERE updated_at > :last_processed_time
ORDER BY updated_at ASC
LIMIT 100;
```

**Garantias:**
- ✅ At-least-once delivery
- ✅ Ordem preservada por partition key (employee_id)
- ✅ Offset persistido após sucesso

### 2. Publicação no Kafka
```
// Roteamento por tipo de evento
String topic = switch(event.getEventType()) {
case "CREATE" -> "employee-create";
case "UPDATE" -> "employee-update";
case "DELETE" -> "employee-delete";
};

// Garantias de entrega
ProducerConfig config = {
acks: "all", // RF=3
retries: 3,
enable.idempotence: true // Exatamente uma vez
};
```
### 3. Consumo e Validação
```
@KafkaListener(topics = {"employee-create", "employee-update", "employee-delete"})
public void consume(@Payload EmployeeEventDTO event, Acknowledgment ack) {
    ValidationResult result = validationEngine.validate(event); // 3 camadas

    if (result.isValid()) {
        persistenceService.persist(event);
        ack.acknowledge(); // Commit manual
    } else {
        dlqService.sendToDLQ(event, result.getErrors());
        ack.acknowledge(); // Não bloqueia fila
    }
}
```

### 4. Persistência com Versionamento
```
-- Atualização otimista
UPDATE public.employees
SET
salary = :new_salary,
version = version + 1,
updated_at = NOW()
WHERE
source_id = :employee_id
AND version = :expected_version;

-- Histórico automático via trigger
INSERT INTO audit.employees_history (...);
```

---

## Qualidade e Testes

### Cobertura Atual

| Componente | Testes | Coverage | Status |
|------------|--------|----------|--------|
| **Producer** | 18 unitários | 82% | ✅ |
| **Consumer** | 35 unitários | 78% | ✅ |
| **E2E** | 23 integração | 100% fluxos | ✅ |
| **CI/CD** | GitHub Actions | Automatizado | ✅ |

### Estratégia de Testes
```
┌─────────────────────────────────────┐
│ Testes End-to-End (23 testes)       │ ← 100% fluxos críticos
│ Testcontainers (Kafka + PostgreSQL) │
├─────────────────────────────────────┤
│ Testes de Integração                │ ← APIs REST + Kafka
├─────────────────────────────────────┤
│ Testes Unitários (53 testes)        │ ← Lógica de negócio
└─────────────────────────────────────┘
```

---

## Observabilidade

### Métricas Customizadas (15 total)

#### Producer Metrics
1. `events_published_total` - Total de eventos publicados
2. `cdc_polling_duration_seconds` - Latência CDC (histogram)
3. `kafka_publish_errors_total` - Erros de publicação

#### Consumer Metrics
4. `events_consumed_total` - Total consumido
5. `validation_success_total` - Validações OK
6. `validation_failure_total` - Validações falhadas
7. `validation_duration_seconds` - Tempo de validação
8. `dlq_events_pending` - Eventos na DLQ
9. `persistence_duration_seconds` - Tempo persistência

### Alertas Configurados (15 regras)

| Alerta | Condição | Severidade | Ação |
|--------|----------|------------|------|
| `ProducerServiceDown` | up == 0 por 1m | CRITICAL | Restart automático |
| `HighErrorRate` | Taxa erro > 5% por 5m | CRITICAL | Investigar logs |
| `CDCHighLatency` | P95 > 10s por 5m | WARNING | Tuning DB |
| `DLQAccumulating` | DLQ > 100 por 10m | WARNING | Reprocessar |

---

## Segurança e Conformidade

### LGPD
- ✅ **Audit Trail:** Histórico completo de mudanças
- ✅ **Soft Delete:** Dados preservados para auditoria
- ✅ **Correlation ID:** Rastreabilidade ponta-a-ponta

### eSocial
- ✅ **Validações:** 11 regras implementadas (6 estruturais + 5 negócio)
- ⏳ **XSD Schema:** Planejado para Sprint 4
- ⏳ **Certificado Digital:** Planejado para produção

---

## Escalabilidade e Performance

### Capacidade Atual

| Métrica | Valor Atual | Limite Teórico | Próximos Passos |
|---------|-------------|----------------|-----------------|
| **Throughput** | 1.000 evt/s | 10.000 evt/s | Adicionar partições |
| **Latência P95** | 50ms | 10ms | Tuning JVM |
| **Disponibilidade** | 99.5% | 99.99% | Cluster Kubernetes |

### Plano de Escala
```
Fase 1 (Atual): 1.000 evt/s
├── 3 Kafka brokers
├── 1 Producer instance
└── 1 Consumer instance

Fase 2 (Produção): 10.000 evt/s
├── 5 Kafka brokers
├── 3 Producer instances (load balanced)
└── 5 Consumer instances (consumer group)

Fase 3 (Enterprise): 100.000 evt/s
├── Kafka + Schema Registry
├── Producer: Auto-scaling (3-10 pods)
└── Consumer: Auto-scaling (5-20 pods)
```

---

## Roadmap Técnico

### Sprint 3 (Atual) ✅
- Testes E2E com Testcontainers
- Sistema de alertas completo
- CI/CD com GitHub Actions
- Documentação arquitetural

### Produção (Futuro)
- Migração CDC para Debezium
- Validação XSD eSocial
- Segurança TLS + SASL
- Backup e DR

- Kubernetes + Helm Charts
- Service Mesh (Istio)
- Certificado Digital A1/A3
- Integração real eSocial

---

## Referências

- [Documentação C4 Model](../architecture/)
- [ADRs Completos](../adr/)
- [Guia de Testes](../../consumer-service/src/test/java/README.md)
- [CI/CD Setup](../CI_CD_SETUP.md)
- [GitHub Repository](https://github.com/marciokuroki/etl-kafka-esocial)

---

## Glossário

- **CDC:** Change Data Capture
- **DLQ:** Dead Letter Queue
- **RF:** Replication Factor
- **ACID:** Atomicity, Consistency, Isolation, Durability
- **P95:** Percentil 95 (latência)
- **eSocial:** Sistema de Escrituração Digital das Obrigações Fiscais (Governo Federal)





