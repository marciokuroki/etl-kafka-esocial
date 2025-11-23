#!/bin/bash
echo "Coletando métricas de baseline..."

# Throughput (eventos/minuto)

THROUGHPUT=\$(curl -s 'http://localhost:9090/api/v1/query?query=rate(events_published_total[1m])*60' | jq -r '.data.result.value[^1]')
echo "Throughput: \$THROUGHPUT eventos/min"

# Latência P95 (ms)

LATENCY_P95=\$(curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(validation_duration_seconds_bucket[5m]))*1000' | jq -r '.data.result.value[^1]')
echo "Latência P95: \$LATENCY_P95 ms"

# Taxa de sucesso (%)

SUCCESS_RATE=\$(curl -s 'http://localhost:9090/api/v1/query?query=(validation_success_total/(validation_success_total+validation_failure_total))*100' | jq -r '.data.result.value[^1]')
echo "Taxa de Sucesso: \$SUCCESS_RATE %"

# Consumer lag

CONSUMER_LAG=\$(curl -s 'http://localhost:9090/api/v1/query?query=kafka_consumergroup_lag' | jq -r '.data.result.value[^1]')
echo "Consumer Lag: \$CONSUMER_LAG eventos"

echo "Baseline coletado com sucesso!"