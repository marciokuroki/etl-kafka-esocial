#!/usr/bin/env bash
set -e

echo "=== eSocial ETL Kafka - Deploy Automático ==="

echo "1. Construindo imagens do Producer e Consumer"
docker build -t esocial-producer ./producer-service
docker build -t esocial-consumer ./consumer-service

echo "2. Parando containers antigos"
docker-compose down

echo "3. Removendo volumes persistentes (opcional, para limpar dados antigos)"
docker volume rm -f prometheus-data grafana-data || true

echo "4. Subindo containers atualizados"
docker-compose up -d --build

echo "5. Aguardando serviços iniciarem"
sleep 20

echo "6. Validando containers ativos"
docker ps | grep -E 'esocial-producer|esocial-consumer|prometheus|grafana'

echo "7. Validando targets Prometheus"
curl -s http://localhost:9090/targets | jq '.status'

echo "8. Prometheus alerta status"
curl -s http://localhost:9090/alerts | jq '.status'

echo "=== Deploy concluído com sucesso ==="
