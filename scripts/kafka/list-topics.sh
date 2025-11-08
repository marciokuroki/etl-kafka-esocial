#!/bin/bash
# list-topics.sh
# Lista todos os tópicos Kafka

docker exec esocial-kafka-broker-1 \
  kafka-topics --list \
  --bootstrap-server localhost:9092
