# Documentação de Métricas - eSocial Pipeline

## Métricas do Producer Service

### Counters

| Métrica | Descrição | Tags | Tipo |
|---------|-----------|------|------|
| `events.published.total` | Total de eventos publicados | service, event_type, topic | Counter |
| `events.failed.total` | Total de falhas na publicação | service, event_type | Counter |
| `cdc.records.detected` | Mudanças detectadas pelo CDC | service | Counter |

### Timers

| Métrica | Descrição | Percentis | Tipo |
|---------|-----------|-----------|------|
| `cdc.polling.duration` | Tempo de polling do CDC | P50, P95, P99 | Timer |
| `kafka.publish.duration` | Tempo de publicação no Kafka | P50, P95, P99 | Timer |

### Histograms

| Métrica | Descrição | Buckets | Tipo |
|---------|-----------|---------|------|
| `events.payload.size` | Tamanho dos payloads | bytes | Histogram |

---

## Métricas do Consumer Service

### Counters

| Métrica | Descrição | Tags | Tipo |
|---------|-----------|------|------|
| `events.consumed.total` | Total de eventos consumidos | service, event_type, topic | Counter |
| `validation.success.total` | Validações bem-sucedidas | service, event_type | Counter |
| `validation.failure.total` | Validações que falharam | service, event_type, severity | Counter |

### Timers

| Métrica | Descrição | Percentis | Tipo |
|---------|-----------|-----------|------|
| `validation.duration` | Tempo de validação | P50, P95, P99 | Timer |
| `persistence.duration` | Tempo de persistência no DB | P50, P95, P99 | Timer |

### Gauges

| Métrica | Descrição | Tags | Tipo |
|---------|-----------|------|------|
| `dlq.events.pending` | Eventos pendentes na DLQ | service, severity | Gauge |

### Histograms

| Métrica | Descrição | Buckets | Tipo |
|---------|-----------|---------|------|
| `events.payload.size` | Tamanho dos payloads recebidos | bytes | Histogram |

---

## Queries PromQL Úteis

### Taxa de publicação (eventos/min)
```
rate(events_published_total[1m]) * 60
```
### Taxa de erro (%)
```
(rate(validation_failure_total[5m]) / rate(events_consumed_total[5m])) * 100
```
### Latência P95 de validação
```
histogram_quantile(0.95, rate(validation_duration_bucket[5m]))
```
### Eventos na DLQ (últimas 24h)
```
dlq_events_pending{severity="high"}
```

## Evolução das Métricas

### Sprint 1 → Sprint 3

As métricas básicas da Sprint 1 foram **mantidas para retrocompatibilidade** e **complementadas** com métricas detalhadas na Sprint 3:

| Sprint 1 (Básico) | Sprint 3 (Detalhado) | Diferença |
|-------------------|----------------------|-----------|
| `events.published` | `events.published.total` | Adiciona tags: event_type, topic |
| `events.failed` | `events.failed.total` | Adiciona tags: event_type, topic, error_type |
| - | `kafka.publish.duration` | Nova: latência de publicação |
| - | `events.payload.size` | Nova: tamanho de payloads |

### Recomendações de Uso

- **Alertas críticos**: Use métricas básicas (`events.published`, `events.failed`)
  - Mais estáveis, menos cardinalidade
  - Exemplo: `rate(events_failed[5m]) > 10`

- **Troubleshooting**: Use métricas detalhadas (`events.published.total`)
  - Filtros granulares por tipo/tópico
  - Exemplo: `events_published_total{event_type="CREATE", topic="employee-create"}`

- **Performance**: Use timers e histograms
  - Análise de percentis (P95, P99)
  - Exemplo: `histogram_quantile(0.95, kafka_publish_duration_bucket)`


## Métricas do CDC (Change Data Capture)

### Evolução Sprint 1 → Sprint 3

| Sprint 1 (Básico) | Sprint 3 (Detalhado) | Diferença |
|-------------------|----------------------|-----------|
| `cdc.records.processed` | `cdc.records.processed.total` | Adiciona tag: event_type |
| - | `cdc.records.detected` | Nova: registros encontrados no polling |
| - | `cdc.polling.duration` | Nova: latência do polling |
| - | `cdc.errors.total` | Nova: erros no CDC |
| - | `cdc.processing.failures` | Nova: falhas em registros individuais |

### Exemplo de Dashboard Grafana

**Painel: "CDC Performance"**
#### Taxa de detecção (eventos/min)
```
rate(cdc_records_detected[1m]) * 60
```
#### Taxa de processamento (eventos/min)
```
rate(cdc_records_processed[1m]) * 60
```
#### Latência P95 do polling
```
histogram_quantile(0.95, rate(cdc_polling_duration_bucket[5m]))
```
#### Taxa de sucesso
```
(rate(cdc_records_processed[5m]) / rate(cdc_records_detected[5m])) * 100
```