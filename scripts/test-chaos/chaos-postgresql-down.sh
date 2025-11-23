#!/bin/bash

# Chaos Test: PostgreSQL Down

echo "========================================="
echo "CHAOS TEST: PostgreSQL Indisponível"
echo "========================================="
echo ""

# Coletar contadores iniciais

echo "1. Coletando contadores iniciais..."
INITIAL_CONSUMED=\$(curl -s http://localhost:8082/actuator/prometheus | grep "events_consumed_total" | awk '{print \$2}')
echo "Eventos consumidos inicialmente: \$INITIAL_CONSUMED"
echo ""

# Derrubar PostgreSQL

echo "2. Derrubando PostgreSQL..."
docker stop esocial-postgres-db
echo "PostgreSQL parado!"
echo ""

# Aguardar

echo "3. Aguardando 10 segundos..."
sleep 10
echo ""

# Verificar health do Consumer

echo "4. Verificando health do Consumer..."
curl -s http://localhost:8082/actuator/health | jq
echo "(Status deve ser DOWN - db DOWN)"
echo ""

# Verificar consumer lag crescendo

echo "5. Observando consumer lag (60 segundos)..."
for i in {1..6}; do
LAG=\$(docker exec esocial-kafka-broker-1 kafka-consumer-groups \
--bootstrap-server localhost:9092 \
--describe --group esocial-consumer-group 2>/dev/null | grep "employee-create" | awk '{print $5}' | paste -sd+ | bc)
  echo "T+$((\$i*10))s: Consumer Lag = \$LAG eventos"
sleep 10
done
echo ""

# Verificar logs do Consumer (erros de conexão)

echo "6. Verificando logs do Consumer..."
docker logs esocial-consumer-service --tail=20 | grep -i "postgres\|connection"
echo ""

# Restaurar PostgreSQL

echo "7. Restaurando PostgreSQL..."
docker start esocial-postgres-db
echo "Aguardando inicialização (30s)..."
sleep 30
echo ""

# Verificar recovery do Consumer

echo "8. Verificando recovery do Consumer..."
curl -s http://localhost:8082/actuator/health | jq .status
echo "(Status deve voltar para UP)"
echo ""

# Aguardar reprocessamento

echo "9. Aguardando reprocessamento (60s)..."
sleep 60
echo ""

# Verificar lag voltou ao normal

echo "10. Verificando consumer lag..."
LAG_FINAL=\$(docker exec esocial-kafka-broker-1 kafka-consumer-groups \
--bootstrap-server localhost:9092 \
--describe --group esocial-consumer-group | grep "employee-create" | awk '{print \$5}' | paste -sd+ | bc)
echo "Consumer Lag final: \$LAG_FINAL eventos"
echo ""

# Validar integridade (contar eventos processados)

FINAL_CONSUMED=\$(curl -s http://localhost:8082/actuator/prometheus | grep "events_consumed_total" | awk '{print $2}')
PROCESSED_DURING_OUTAGE=$((FINAL_CONSUMED - INITIAL_CONSUMED))
echo "11. Eventos processados após recovery: \$PROCESSED_DURING_OUTAGE"
echo ""

echo "========================================="
echo "TESTE CONCLUÍDO"
echo "========================================="