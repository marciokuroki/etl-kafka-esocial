# Dashboards Grafana do Pipeline eSocial

## Dashboards Disponíveis

1. **Visão Geral do Pipeline**
   - Taxa de eventos publicados, consumidos e processados
   - Latência P95 de publicação Kafka
   - Eventos pendentes na DLQ

2. **Detalhamento por Tipo de Evento**
   - Distribuição por evento: CREATE, UPDATE, DELETE
   - Taxas de erro e severidade

3. **Validação e Erros**
   - Falhas de validação por severidade
   - Eventos pendentes na DLQ detalhados

## Como importar

Importação manual via UI ou provisionamento automático (recomendado).

## Queries PromQL úteis

- Taxa de eventos publicados:
```
rate(events_published_total{service="producer"}[1m]) * 60
```
- Latência P95 da validação:
```
histogram_quantile(0.95, rate(validation_duration_seconds_bucket[5m]))
```
