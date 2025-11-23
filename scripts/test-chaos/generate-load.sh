#!/bin/bash

# Gerar 8.000 eventos em 10 minutos (800 evt/min = 10x normal)

EVENTS=8000
DURATION_SECONDS=600  \# 10 minutos

echo "Gerando \$EVENTS eventos em \$DURATION_SECONDS segundos..."

for i in \$(seq 1 \$EVENTS); do

# Inserir evento no PostgreSQL (origem)

docker exec esocial-postgres-db psql -U esocial_user -d esocial -c \
"INSERT INTO source.employees VALUES (
'LOADTEST$i',
      '$(printf "%011d" \$i)',  -- CPF sequencial
'10011223344',
'Load Test User \$i',
'1990-01-01',
'2024-01-01',
NULL,
'Tester',
'QA',
5000.00,
'ACTIVE',
NOW(),
NOW()
);" > /dev/null 2>\&1

# Sleep para distribuir carga

sleep \$(echo "scale=3; \$DURATION_SECONDS / \$EVENTS" | bc)

# Progresso

if [ \$((i % 100)) -eq 0 ]; then
echo "Progresso: \$i / \$EVENTS eventos"
fi
done

echo "Geração de carga concluída!"