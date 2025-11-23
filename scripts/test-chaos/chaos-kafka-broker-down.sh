#!/bin/bash

# Chaos Test: Kafka Broker Down

echo "========================================="
echo "CHAOS TEST: Kafka Broker Down"
echo "========================================="
echo ""

# Coletar baseline

echo "1. Coletando baseline..."
BASELINE_THROUGHPUT=\$(curl -s 'http://localhost:9090/api/v1/query?query=rate(events_published_total[1m])*60' | jq -r '.data.result.value[^1]')
echo "Baseline Throughput: \$BASELINE_THROUGHPUT evt/min"
echo ""

# Derrubar broker 2 (de 3)

echo "2. Derrubando Kafka Broker 2..."
docker stop esocial-kafka-broker-2
echo "Broker 2 parado!"
echo ""

# Aguardar rebalanceamento (30 segundos)

echo "3. Aguardando rebalanceamento (30s)..."
sleep 30
echo ""

# Verificar status

echo "4. Verificando status do cluster..."
docker exec esocial-kafka-broker-1 kafka-broker-api-versions \
--bootstrap-server localhost:9092 | grep -c "ApiVersion"
echo "(Deve mostrar 2 brokers ativos)"
echo ""

# Medir impacto

echo "5. Medindo impacto (60 segundos de observação)..."
sleep 60

IMPACTED_THROUGHPUT=\$(curl -s 'http://localhost:9090/api/v1/query?query=rate(events_published_total[1m])*60' | jq -r '.data.result.value[^1]')
echo "Throughput durante falha: \$IMPACTED_THROUGHPUT evt/min"

DEGRADATION=\$(echo "scale=2; (1 - \$IMPACTED_THROUGHPUT / \$BASELINE_THROUGHPUT) * 100" | bc)
echo "Degradação: \$DEGRADATION%"
echo ""

# Verificar perda de dados

echo "6. Verificando perda de dados..."
PRODUCER_COUNT=\$(curl -s http://localhost:8081/actuator/prometheus | grep "events_published_total" | awk '{print $2}')
CONSUMER_COUNT=$(curl -s http://localhost:8082/actuator/prometheus | grep "events_consumed_total" | awk '{print $2}')
DIFF=$((PRODUCER_COUNT - CONSUMER_COUNT))
echo "Eventos produzidos: \$PRODUCER_COUNT"
echo "Eventos consumidos: \$CONSUMER_COUNT"
echo "Diferença (lag): \$DIFF"
echo ""

# Restaurar broker

echo "7. Restaurando Kafka Broker 2..."
docker start esocial-kafka-broker-2
echo "Aguardando inicialização (60s)..."
sleep 60
echo ""

# Verificar recovery

echo "8. Verificando recovery..."
docker exec esocial-kafka-broker-1 kafka-broker-api-versions \
--bootstrap-server localhost:9092 | grep -c "ApiVersion"
echo "(Deve mostrar 3 brokers ativos)"
echo ""

# Validar ISR (In-Sync Replicas)

echo "9. Validando ISR..."
docker exec esocial-kafka-broker-1 kafka-topics \
--bootstrap-server localhost:9092 \
--describe --topic employee-create | grep "Isr:"
echo "(Todos os 3 brokers devem estar em ISR)"
echo ""

echo "========================================="
echo "TESTE CONCLUÍDO"
echo "========================================="