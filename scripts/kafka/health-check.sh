#!/bin/bash
# health-check.sh
# Health check completo do ambiente

echo "========================================="
echo "  Health Check Completo - Ambiente POC"
echo "========================================="

# Zookeeper - Usando comando srvr
echo ""
echo "1. Zookeeper:"
if docker exec esocial-zookeeper bash -c "echo srvr | nc localhost 2181" 2>/dev/null | grep -q "Mode"; then
    echo "  ✓ Zookeeper OK"
else
    echo "  ✗ Zookeeper FALHOU"
fi

# Kafka Brokers
echo ""
echo "2. Kafka Brokers:"
docker exec esocial-kafka-broker-1 kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1 && echo "  ✓ Broker 1 OK" || echo "  ✗ Broker 1 FALHOU"
docker exec esocial-kafka-broker-2 kafka-broker-api-versions --bootstrap-server localhost:9093 > /dev/null 2>&1 && echo "  ✓ Broker 2 OK" || echo "  ✗ Broker 2 FALHOU"
docker exec esocial-kafka-broker-3 kafka-broker-api-versions --bootstrap-server localhost:9094 > /dev/null 2>&1 && echo "  ✓ Broker 3 OK" || echo "  ✗ Broker 3 FALHOU"

# Oracle
echo ""
echo "3. Oracle Database:"
docker exec esocial-oracle-db bash -c "echo 'SELECT 1 FROM DUAL;' | sqlplus -S / as sysdba" > /dev/null 2>&1 && echo "  ✓ Oracle OK" || echo "  ✗ Oracle FALHOU (pode estar inicializando)"

# PostgreSQL
echo ""
echo "4. PostgreSQL:"
docker exec esocial-postgres-db pg_isready -U esocial_user > /dev/null 2>&1 && echo "  ✓ PostgreSQL OK" || echo "  ✗ PostgreSQL FALHOU"

echo ""
echo "========================================="
echo "  Health Check Concluído"
echo "========================================="
