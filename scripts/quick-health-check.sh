#!/bin/bash

#######################################################################
# Quick Health Check - Sistema de Alertas
# Validação rápida (< 10 segundos)
#######################################################################

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

check() {
    if eval "$2" &>/dev/null; then
        echo -e "${GREEN}✓${NC} $1"
        return 0
    else
        echo -e "${RED}✗${NC} $1"
        return 1
    fi
}

echo "Quick Health Check - Sistema de Alertas"
echo "========================================"

check "Prometheus" "curl -sf http://localhost:9090/-/healthy"
check "Alertmanager" "curl -sf http://localhost:9093/-/healthy"
check "Producer" "curl -sf http://localhost:8081/actuator/health | jq -e '.status==\"UP\"'"
check "Consumer" "curl -sf http://localhost:8082/actuator/health | jq -e '.status==\"UP\"'"
check "Webhook" "curl -sf http://localhost:5001/health"

echo ""
echo "Alertas Ativos:"
curl -s http://localhost:9090/api/v1/alerts | jq -r '.data.alerts[] | select(.state=="firing") | "  - \(.labels.alertname) (\(.labels.severity))"' || echo "  Nenhum"

echo ""
echo "Targets:"
curl -s http://localhost:9090/api/v1/targets | jq -r '.data.activeTargets[] | "  \(.labels.job): \(.health)"'
