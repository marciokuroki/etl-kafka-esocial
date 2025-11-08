#!/bin/bash
# validate-pipeline.sh

echo "========================================="
echo "  Validação Completa do Pipeline ETL"
echo "========================================="

# 1. Status dos containers
echo ""
echo "1. Status dos Containers:"
docker-compose ps | grep -E "(producer|consumer|kafka|postgres)" | awk '{print $1, $5}'

# 2. Health dos serviços
echo ""
echo "2. Health Checks:"
echo -n "  Producer: "
curl -s http://localhost:8081/actuator/health | jq -r .status
echo -n "  Consumer: "
curl -s http://localhost:8082/actuator/health | jq -r .status

# 3. Métricas
echo ""
echo "3. Métricas:"
PUBLISHED=$(curl -s http://localhost:8081/actuator/prometheus | grep "events_published_total" | awk '{print $2}')
CONSUMED=$(curl -s http://localhost:8082/actuator/prometheus | grep "events_consumed_total" | awk '{print $2}')
PROCESSED=$(curl -s http://localhost:8082/actuator/prometheus | grep "events_processed_total" | awk '{print $2}')

echo "  Eventos Publicados: $PUBLISHED"
echo "  Eventos Consumidos: $CONSUMED"
echo "  Eventos Processados: $PROCESSED"

# 4. Dados no PostgreSQL
echo ""
echo "4. Dados Persistidos:"
TOTAL_EMPLOYEES=$(docker exec esocial-postgres-db psql -U esocial_user -d esocial -t -c "SELECT COUNT(*) FROM public.employees;")
TOTAL_HISTORY=$(docker exec esocial-postgres-db psql -U esocial_user -d esocial -t -c "SELECT COUNT(*) FROM audit.employees_history;")
echo "  Colaboradores: $TOTAL_EMPLOYEES"
echo "  Histórico: $TOTAL_HISTORY"

# 5. Validação
echo ""
echo "5. Validação:"
TOTAL_ERRORS=$(curl -s http://localhost:8082/api/v1/validation/dashboard | jq -r .validation.totalErrors)
echo "  Total de Erros: $TOTAL_ERRORS"

echo ""
echo "========================================="
echo "  ✓ Validação Concluída"
echo "========================================="
