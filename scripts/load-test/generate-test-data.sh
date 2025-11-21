#!/bin/bash

echo "=========================================="
echo "  Gerador de Carga de Teste - eSocial"
echo "=========================================="

# Configurações
DB_CONTAINER="esocial-postgres-db"
DB_USER="esocial_user"
DB_NAME="esocial"
NUM_RECORDS=${1:-10}  # Default: 10 registros

echo "📊 Gerando $NUM_RECORDS eventos de teste..."

# Função para gerar CPF aleatório
generate_cpf() {
    echo "$(printf "%011d" $((RANDOM * RANDOM % 99999999999)))"
}

# Função para gerar PIS aleatório
generate_pis() {
    echo "$(printf "%011d" $((RANDOM * RANDOM % 99999999999)))"
}

# 1. Inserir novos colaboradores (CREATE - S-2300)
echo ""
echo "1️⃣  Inserindo $NUM_RECORDS novos colaboradores (CREATE)..."
docker exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME << EOF
INSERT INTO source.employees (
    employee_id, cpf, pis, full_name, birth_date, 
    admission_date, job_title, department, salary, 
    status, created_at, updated_at
)
SELECT 
    'EMP-TEST-' || generate_series,
    lpad((12345678900 + generate_series)::text, 11, '0'),
    lpad((10011223300 + generate_series)::text, 11, '0'),
    'Colaborador Teste ' || generate_series,
    DATE '1990-01-01' + (generate_series || ' days')::interval,
    CURRENT_DATE,
    CASE (generate_series % 3)
        WHEN 0 THEN 'Analista de Sistemas'
        WHEN 1 THEN 'Desenvolvedor'
        ELSE 'Gerente de Projetos'
    END,
    CASE (generate_series % 2)
        WHEN 0 THEN 'TI'
        ELSE 'RH'
    END,
    5000.00 + (generate_series * 100),
    'ACTIVE',
    NOW(),
    NOW()
FROM generate_series(1, $NUM_RECORDS);
EOF

echo "✅ $NUM_RECORDS colaboradores inseridos"

# 2. Aguardar CDC detectar (polling = 5s)
echo ""
echo "⏳ Aguardando CDC processar... (10s)"
sleep 10

# 3. Atualizar salários (UPDATE - S-2400)
echo ""
echo "2️⃣  Atualizando salários de 5 colaboradores (UPDATE)..."
docker exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME << EOF
UPDATE source.employees
SET salary = salary + 500.00,
    updated_at = NOW()
WHERE employee_id LIKE 'EMP-TEST-%'
LIMIT 5;
EOF

echo "✅ 5 salários atualizados"

# 4. Aguardar processamento
echo ""
echo "⏳ Aguardando processamento... (10s)"
sleep 10

# 5. Desligar 2 colaboradores (DELETE - S-2420)
echo ""
echo "3️⃣  Desligando 2 colaboradores (DELETE)..."
docker exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME << EOF
UPDATE source.employees
SET status = 'INACTIVE',
    termination_date = CURRENT_DATE,
    updated_at = NOW()
WHERE employee_id LIKE 'EMP-TEST-%'
AND status = 'ACTIVE'
LIMIT 2;
EOF

echo "✅ 2 colaboradores desligados"

# 6. Aguardar processamento final
echo ""
echo "⏳ Aguardando processamento final... (10s)"
sleep 10

# 7. Exibir métricas
echo ""
echo "=========================================="
echo "  📊 MÉTRICAS DO TESTE"
echo "=========================================="

echo ""
echo "🔹 Producer Metrics:"
curl -s http://localhost:8081/actuator/prometheus | grep -E "events_published_total|cdc_records_detected" | grep -v "#"

echo ""
echo "🔹 Consumer Metrics:"
curl -s http://localhost:8082/actuator/prometheus | grep -E "events_consumed_total|validation_success_total|validation_failure_total" | grep -v "#"

echo ""
echo "🔹 DLQ Status:"
curl -s http://localhost:8082/actuator/prometheus | grep "dlq_events_pending" | grep -v "#"

echo ""
echo "=========================================="
echo "✅ Teste concluído!"
echo "=========================================="
echo ""
echo "📌 Próximos passos:"
echo "   - Acessar Grafana: http://localhost:3000"
echo "   - Acessar Prometheus: http://localhost:9090"
echo "   - Verificar logs: docker logs -f esocial-consumer-service"
