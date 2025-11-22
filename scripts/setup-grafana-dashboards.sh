#!/bin/bash

#######################################################################
# Setup de Dashboards Grafana
# Pipeline ETL eSocial
#######################################################################

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}🎨 Setup de Dashboards Grafana${NC}"
echo "======================================"

# Verificar se Grafana está rodando
echo -e "\n${YELLOW}⏳${NC} Verificando Grafana..."
if ! curl -sf http://localhost:3000/api/health > /dev/null 2>&1; then
    echo "❌ Grafana não está acessível em http://localhost:3000"
    echo "   Execute: docker-compose up -d grafana"
    exit 1
fi
echo -e "${GREEN}✅${NC} Grafana está rodando"

# Credenciais (alterar se necessário)
GRAFANA_URL="http://localhost:3000"
GRAFANA_USER="${GRAFANA_USER:-admin}"
GRAFANA_PASS="${GRAFANA_PASS:-GrafanaPassword123!}"

# Função para importar dashboard
import_dashboard() {
    local dashboard_file=$1
    local dashboard_name=$(basename "$dashboard_file" .json)
    
    echo -e "\n${YELLOW}⏳${NC} Importando dashboard: $dashboard_name"
    
    # Ler JSON e preparar payload
    dashboard_json=$(cat "$dashboard_file")
    payload=$(jq -n --argjson dashboard "$dashboard_json" '{
        dashboard: $dashboard.dashboard,
        overwrite: true,
        folderId: 0
    }')
    
    # Importar via API
    response=$(curl -sf -X POST "$GRAFANA_URL/api/dashboards/db" \
        -H "Content-Type: application/json" \
        -u "$GRAFANA_USER:$GRAFANA_PASS" \
        -d "$payload" 2>&1)
    
    if echo "$response" | jq -e '.status == "success"' > /dev/null 2>&1; then
        uid=$(echo "$response" | jq -r '.uid')
        echo -e "${GREEN}✅${NC} Dashboard importado: $dashboard_name (UID: $uid)"
        echo "   URL: $GRAFANA_URL/d/$uid"
    else
        echo -e "${GREEN}⚠️${NC}  Dashboard pode já existir ou será provisionado automaticamente"
    fi
}

# Importar dashboards
echo -e "\n${CYAN}📊 Importando Dashboards${NC}"
echo "------------------------"

if [ -d "config/grafana/dashboards" ]; then
    for dashboard in config/grafana/dashboards/*.json; do
        if [ -f "$dashboard" ]; then
            import_dashboard "$dashboard"
        fi
    done
else
    echo "❌ Diretório config/grafana/dashboards não encontrado"
    exit 1
fi

# Criar pasta se não existir
echo -e "\n${YELLOW}⏳${NC} Criando pasta 'eSocial ETL'..."
folder_response=$(curl -sf -X POST "$GRAFANA_URL/api/folders" \
    -H "Content-Type: application/json" \
    -u "$GRAFANA_USER:$GRAFANA_PASS" \
    -d '{"title": "eSocial ETL"}' 2>&1) || true

echo -e "${GREEN}✅${NC} Pasta criada/verificada"

# Configurar datasource Prometheus (se necessário)
echo -e "\n${YELLOW}⏳${NC} Verificando datasource Prometheus..."
datasource_check=$(curl -sf "$GRAFANA_URL/api/datasources/name/Prometheus" \
    -u "$GRAFANA_USER:$GRAFANA_PASS" 2>&1) || datasource_exists="false"

if [ "$datasource_exists" = "false" ]; then
    echo -e "${YELLOW}⏳${NC} Criando datasource Prometheus..."
    curl -sf -X POST "$GRAFANA_URL/api/datasources" \
        -H "Content-Type: application/json" \
        -u "$GRAFANA_USER:$GRAFANA_PASS" \
        -d '{
            "name": "Prometheus",
            "type": "prometheus",
            "url": "http://prometheus:9090",
            "access": "proxy",
            "isDefault": true
        }' > /dev/null 2>&1
    echo -e "${GREEN}✅${NC} Datasource criado"
else
    echo -e "${GREEN}✅${NC} Datasource já existe"
fi

# Resumo
echo -e "\n${CYAN}======================================"
echo "✅ Setup Concluído!"
echo "======================================${NC}"
echo ""
echo "🌐 Acessar Grafana:"
echo "   URL: http://localhost:3000"
echo "   Usuário: $GRAFANA_USER"
echo "   Senha: $GRAFANA_PASS"
echo ""
echo "📊 Dashboards Disponíveis:"
echo "   - Pipeline ETL eSocial - Overview"
echo "   - Sistema de Alertas - Monitor"
echo "   - Pipeline ETL - Performance"
echo ""
echo "💡 Dica: Dashboards também são provisionados automaticamente"
echo "   ao reiniciar o container Grafana"
