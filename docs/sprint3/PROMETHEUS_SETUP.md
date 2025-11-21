# Prometheus - Configuração e Uso

## Visão Geral

O Prometheus coleta métricas dos serviços Producer e Consumer a cada 10 segundos, armazenando dados por 15 dias.

## Endpoints

- **UI do Prometheus**: http://localhost:9090
- **Targets**: http://localhost:9090/targets
- **Alertas**: http://localhost:9090/alerts
- **Métricas**: http://localhost:9090/graph

## Targets Configurados

| Job | Endpoint | Intervalo | Status |
|-----|----------|-----------|--------|
| `prometheus` | localhost:9090 | 15s | ✅ Self-monitoring |
| `kafka-brokers` | kafka-broker-{1,2,3}:19092-94 | 30s | ⚠️ Requer JMX Exporter |
| `producer-service` | producer-service:8081/actuator/prometheus | 10s | ✅ Ativo |
| `consumer-service` | consumer-service:8082/actuator/prometheus | 10s | ✅ Ativo |

## Regras de Alerta

### Críticos (severity: critical)

| Alerta | Condição | Tempo | Ação |
|--------|----------|-------|------|
| `ProducerHighErrorRate` | Taxa de erro > 5% | 5min | Verificar logs Producer |
| `ConsumerHighErrorRate` | Taxa de validação falha > 5% | 5min | Verificar regras de validação |
| `ProducerServiceDown` | Service indisponível | 1min | Reiniciar container |
| `ConsumerServiceDown` | Service indisponível | 1min | Reiniciar container |
| `DLQCritical` | DLQ > 500 eventos | 5min | Reprocessar DLQ |

### Warnings (severity: warning)

| Alerta | Condição | Tempo | Ação |
|--------|----------|-------|------|
| `CDCHighLatency` | P95 CDC > 10s | 5min | Otimizar queries CDC |
| `DLQAccumulating` | DLQ > 100 eventos | 10min | Investigar erros |
| `ValidationLatencyHigh` | P95 validação > 5s | 5min | Otimizar regras |
| `LowThroughput` | < 10 eventos/min | 10min | Verificar infraestrutura |

## Queries Úteis

### Taxa de Publicação (eventos/min)
```
rate(events_published_total{service="producer"}[1m]) * 60
```

### Taxa de Erro (%)
```
(rate(validation_failure_total[5m]) / rate(events_consumed_total[5m])) * 100
```

### Latência P95 do Pipeline Completo
```
histogram_quantile(0.95, 
  rate(kafka_publish_duration_seconds_bucket[5m])
  + rate(validation_duration_seconds_bucket[5m])
  + rate(persistence_duration_seconds_bucket[5m])
)
```

### Eventos na DLQ
```
dlq_events_pending{severity="high"}
```

## Troubleshooting

### Targets não coletando métricas

```
# Verificar conectividade
docker exec -it esocial-prometheus wget -O- http://producer-service:8081/actuator/prometheus

# Verificar logs do Prometheus
docker logs esocial-prometheus | grep ERROR
```

### Alertas não disparando

```
# Verificar regras carregadas
docker exec -it esocial-prometheus promtool check rules /etc/prometheus/alerts.yml

# Recarregar configuração
docker exec -it esocial-prometheus kill -HUP 1
```

## Manutenção

### Limpeza de Dados Antigos

Retenção configurada: **15 dias** (automático)

### Backup de Métricas

```
# Exportar snapshot
docker exec -it esocial-prometheus \
  curl -XPOST http://localhost:9090/api/v1/admin/tsdb/snapshot

# Copiar snapshot
docker cp esocial-prometheus:/prometheus/snapshots/. ./backups/prometheus/
```