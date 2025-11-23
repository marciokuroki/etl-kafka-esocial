#!/bin/bash

# Chaos Test: Alta Latência no Sistema Origem

echo "========================================="
echo "CHAOS TEST: Alta Latência (Sistema Origem)"
echo "========================================="
echo ""

# Baseline

echo "1. Coletando baseline CDC latency..."
BASELINE_CDC_LATENCY=\$(curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(cdc_polling_duration_seconds_bucket[5m]))*1000' | jq -r '.data.result.value[^1]')
echo "Baseline CDC Latency P95: \$BASELINE_CDC_LATENCY ms"
echo ""

# Ativar latência (via Toxiproxy)

echo "2. Ativando latência de 5 segundos no PostgreSQL..."
curl -X POST http://localhost:8474/proxies/postgres-proxy/toxics \
-d '{
"name": "latency",
"type": "latency",
"attributes": {
"latency": 5000
}
}'
echo "Latência ativada!"
echo ""

# Aguardar

echo "3. Aguardando impacto (60 segundos)..."
sleep 60
echo ""

# Medir impacto

echo "4. Medindo impacto na latência CDC..."
IMPACTED_CDC_LATENCY=\$(curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(cdc_polling_duration_seconds_bucket[5m]))*1000' | jq -r '.data.result.value[^1]')
echo "CDC Latency P95 durante falha: \$IMPACTED_CDC_LATENCY ms"
echo ""

# Verificar throughput do Producer

echo "5. Verificando throughput do Producer..."
THROUGHPUT=\$(curl -s 'http://localhost:9090/api/v1/query?query=rate(events_published_total[1m])*60' | jq -r '.data.result.value[^1]')
echo "Throughput Producer: \$THROUGHPUT evt/min"
echo ""

# Verificar se Consumer foi afetado

echo "6. Verificando Consumer (não deve ser afetado)..."
CONSUMER_LATENCY=\$(curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(validation_duration_seconds_bucket[5m]))*1000' | jq -r '.data.result.value[^1]')
echo "Consumer Latency P95: \$CONSUMER_LATENCY ms (deve estar normal)"
echo ""

# Remover latência

echo "7. Removendo latência..."
curl -X DELETE http://localhost:8474/proxies/postgres-proxy/toxics/latency
echo "Latência removida!"
echo ""

# Aguardar recovery

echo "8. Aguardando recovery (60s)..."
sleep 60
echo ""

# Validar recovery

echo "9. Validando recovery..."
RECOVERY_CDC_LATENCY=\$(curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(cdc_polling_duration_seconds_bucket[5m]))*1000' | jq -r '.data.result.value[^1]')
echo "CDC Latency P95 após recovery: \$RECOVERY_CDC_LATENCY ms"
echo ""

echo "========================================="
echo "TESTE CONCLUÍDO"
echo "========================================="