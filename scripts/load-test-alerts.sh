#!/bin/bash

#######################################################################
# Teste de Carga - Dispara múltiplos alertas
#######################################################################

echo "🧪 Teste de Carga - Sistema de Alertas"
echo "======================================="

echo -e "\n1️⃣ Gerando eventos inválidos (HighValidationErrorRate)..."
for i in {1..50}; do
    docker exec esocial-postgres-db psql -U esocial_user -d esocial -c "
        INSERT INTO source.employees VALUES (
            'LOAD_TEST_$i',
            '123',
            NULL,
            'Load Test $i',
            '2030-01-01',
            '2024-01-01',
            NULL,
            'Test',
            'TI',
            100.00,
            'ACTIVE',
            NOW(),
            NOW()
        );
    " > /dev/null 2>&1
    
    if [ $((i % 10)) -eq 0 ]; then
        echo "  ✓ $i eventos injetados..."
    fi
done
echo "✅ 50 eventos inválidos injetados"

echo -e "\n2️⃣ Aguardando processamento (30s)..."
sleep 30

echo -e "\n3️⃣ Verificando alertas disparados..."
curl -s http://localhost:9090/api/v1/alerts | jq -r '
    .data.alerts[] | 
    select(.state=="firing" or .state=="pending") | 
    "\(.labels.alertname) (\(.state)) - \(.labels.severity)"
' | sort | uniq

echo -e "\n4️⃣ Verificando DLQ..."
dlq_count=$(curl -s http://localhost:8082/api/v1/validation/dlq | jq 'length')
echo "  DLQ: $dlq_count eventos"

echo -e "\n✅ Teste de carga concluído!"
echo "📊 Monitore em: http://localhost:9090/alerts"
