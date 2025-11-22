#!/bin/bash

echo "🧪 Testando Sistema de Alertas"
echo "================================"

# Teste 1: Gerar erros para disparar HighErrorRate
echo -e "\n1️⃣ Teste: Injetar eventos inválidos (HighErrorRate)"
for i in {1..20}; do
  docker exec esocial-postgres-db psql -U esocial_user -d esocial -c "
    INSERT INTO source.employees VALUES (
      'TEST$i',
      '123',  -- CPF inválido (menos de 11 dígitos)
      NULL,
      'Teste Erro $i',
      '1990-01-01',
      '2024-01-01',
      NULL,
      'Teste',
      'TI',
      3000.00,
      'ACTIVE',
      NOW(),
      NOW()
    );
  " > /dev/null 2>&1
done
echo "✅ 20 eventos inválidos injetados"
echo "⏳ Aguarde 5 minutos e verifique alertas em: http://localhost:9093"

# Teste 2: Simular latência (parar consumer temporariamente)
echo -e "\n2️⃣ Teste: Simular consumer lag (HighConsumerLag)"
echo "⏸️  Pausando Consumer Service por 2 minutos..."
docker-compose pause consumer-service
sleep 120
echo "▶️  Retomando Consumer Service..."
docker-compose unpause consumer-service
echo "✅ Teste de lag concluído"

# Teste 3: Derrubar serviço
echo -e "\n3️⃣ Teste: Simular serviço down (ServiceDown)"
echo "🛑 Parando Producer Service por 90 segundos..."
docker-compose stop producer-service
sleep 90
echo "🚀 Reiniciando Producer Service..."
docker-compose start producer-service
echo "✅ Teste de serviço down concluído"

echo -e "\n================================"
echo "✅ Todos os testes executados!"
echo "📊 Verificar alertas em:"
echo "   - Prometheus: http://localhost:9090/alerts"
echo "   - Alertmanager: http://localhost:9093"
echo "   - Webhook Logs: docker logs esocial-webhook-receiver"
