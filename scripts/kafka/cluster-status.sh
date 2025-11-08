#!/bin/bash
# cluster-status.sh
# Verifica status do cluster Kafka

echo "==================================="
echo "Status do Cluster Kafka"
echo "==================================="

echo ""
echo "Brokers ativos:"
docker exec esocial-kafka-broker-1 \
  kafka-broker-api-versions \
  --bootstrap-server localhost:9092 | grep "Broker:"

echo ""
echo "Tópicos criados:"
docker exec esocial-kafka-broker-1 \
  kafka-topics --list \
  --bootstrap-server localhost:9092

echo ""
echo "==================================="
