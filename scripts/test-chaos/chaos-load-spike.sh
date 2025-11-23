#!/bin/bash

# Chaos Test: Pico de Carga (10x Normal)

echo "========================================="
echo "CHAOS TEST: Pico de Carga (10x Normal)"
echo "========================================="
echo ""

# Baseline

echo "1. Coletando baseline..."
./scripts/collect-baseline.sh
echo ""

# Monitorar métricas em background

echo "2. Iniciando monitoramento contínuo..."
./scripts/monitor-metrics.sh \&
MONITOR_PID=\$!
echo "Monitoramento ativo (PID: \$MONITOR_PID)"
echo ""

# Gerar carga massiva

echo "3. Iniciando geração de carga (8.000 eventos em 10 minutos)..."
time ./scripts/generate-load.sh
echo ""

# Aguardar processamento completo

echo "4. Aguardando processamento completo (5 minutos)..."
sleep 300
echo ""

# Parar monitoramento

kill \$MONITOR_PID

# Coletar métricas finais

echo "5. Coletando métricas finais..."
PEAK_THROUGHPUT=\$(curl -s 'http://localhost:9090/api/v1/query?query=max_over_time(rate(events_published_total[1m])[10m:])*60' | jq -r '.data.result.value[^1]')
echo "Peak Throughput: \$PEAK_THROUGHPUT evt/min"

PEAK_LATENCY=\$(curl -s 'http://localhost:9090/api/v1/query?query=max_over_time(histogram_quantile(0.95,rate(validation_duration_seconds_bucket[1m]))[10m:])*1000' | jq -r '.data.result.value[^1]')
echo "Peak Latency P95: \$PEAK_LATENCY ms"

MAX_LAG=\$(curl -s 'http://localhost:9090/api/v1/query?query=max_over_time(kafka_consumergroup_lag[10m:])' | jq -r '.data.result.value[^1]')
echo "Max Consumer Lag: \$MAX_LAG eventos"
echo ""

# Validar integridade

echo "6. Validando integridade..."
PRODUCED=$(docker exec esocial-postgres-db psql -U esocial_user -d esocial -t -c "SELECT COUNT(*) FROM source.employees WHERE employee_id LIKE 'LOADTEST%';")
CONSUMED=$(docker exec esocial-postgres-db psql -U esocial_user -d esocial -t -c "SELECT COUNT(*) FROM public.employees WHERE source_id LIKE 'LOADTEST%';")

echo "Eventos produzidos: \$PRODUCED"
echo "Eventos consumidos: \$CONSUMED"
echo "Taxa de sucesso: \$(echo "scale=2; \$CONSUMED / \$PRODUCED * 100" | bc)%"
echo ""

# Verificar crashes

echo "7. Verificando crashes..."
docker ps | grep esocial-producer-service | grep -q Up \&\& echo "Producer: UP ✅" || echo "Producer: DOWN ❌"
docker ps | grep esocial-consumer-service | grep -q Up \&\& echo "Consumer: UP ✅" || echo "Consumer: DOWN ❌"
echo ""

echo "========================================="
echo "TESTE CONCLUÍDO"
echo "========================================="