#!/bin/bash

#######################################################################
# Script de Validação Completa do Sistema de Alertas
# Pipeline ETL eSocial com Apache Kafka
# 
# Autor: Márcio Kuroki Gonçalves
# Data: 2025-11-22
# Versão: 1.0
#
# Descrição:
#   Valida todos os componentes do sistema de alertas:
#   - Containers e health checks
#   - Conectividade entre serviços
#   - Regras de alerta carregadas
#   - Métricas disponíveis
#   - Integração Prometheus ↔ Alertmanager
#   - Disparo de alertas (teste sintético)
#
# Uso:
#   ./scripts/validate-alerting-system.sh [--quick|--full]
#
#   --quick: Validação rápida (sem testes de disparo)
#   --full:  Validação completa com testes de disparo
#######################################################################

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Contadores
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNING_CHECKS=0

# Modo de execução
MODE="${1:-quick}"

# Arquivo de log
LOG_FILE="validation-$(date +%Y%m%d-%H%M%S).log"

#######################################################################
# FUNÇÕES AUXILIARES
#######################################################################

print_header() {
    echo -e "${BOLD}${BLUE}"
    echo "================================================================================"
    echo "$1"
    echo "================================================================================"
    echo -e "${NC}"
}

print_section() {
    echo -e "\n${BOLD}${CYAN}▶ $1${NC}"
    echo "--------------------------------------------------------------------------------"
}

print_check() {
    echo -e "${YELLOW}⏳${NC} $1..."
}

print_success() {
    ((PASSED_CHECKS++))
    ((TOTAL_CHECKS++))
    echo -e "${GREEN}✅ PASS${NC} - $1"
}

print_fail() {
    ((FAILED_CHECKS++))
    ((TOTAL_CHECKS++))
    echo -e "${RED}❌ FAIL${NC} - $1"
}

print_warning() {
    ((WARNING_CHECKS++))
    ((TOTAL_CHECKS++))
    echo -e "${YELLOW}⚠️  WARN${NC} - $1"
}

print_info() {
    echo -e "${CYAN}ℹ️  INFO${NC} - $1"
}

check_command() {
    if command -v "$1" &> /dev/null; then
        print_success "Comando '$1' disponível"
        return 0
    else
        print_fail "Comando '$1' não encontrado"
        return 1
    fi
}

check_url() {
    local url=$1
    local expected_code=${2:-200}
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>&1)
    
    if [ "$response" -eq "$expected_code" ]; then
        print_success "URL acessível: $url (HTTP $response)"
        return 0
    else
        print_fail "URL retornou código inesperado: $url (HTTP $response, esperado $expected_code)"
        return 1
    fi
}

#######################################################################
# VALIDAÇÕES
#######################################################################

validate_prerequisites() {
    print_section "1. PRÉ-REQUISITOS"
    
    print_check "Verificando comandos necessários"
    check_command "docker" || return 1
    check_command "docker-compose" || return 1
    check_command "curl" || return 1
    check_command "jq" || return 1
    
    print_info "Todos os comandos necessários estão disponíveis"
}

validate_containers() {
    print_section "2. CONTAINERS E SERVIÇOS"
    
    local containers=(
        "esocial-prometheus"
        "esocial-alertmanager"
        "esocial-webhook-receiver"
        "esocial-producer"
        "esocial-consumer"
    )
    
    for container in "${containers[@]}"; do
        print_check "Verificando container $container"
        
        if docker ps --filter "name=$container" --filter "status=running" | grep -q "$container"; then
            print_success "Container $container está rodando"
        else
            print_fail "Container $container NÃO está rodando"
        fi
    done
}

validate_health_checks() {
    print_section "3. HEALTH CHECKS"
    
    print_check "Prometheus Health"
    if response=$(curl -sf http://localhost:9090/-/healthy 2>&1); then
        print_success "Prometheus health check OK"
    else
        print_fail "Prometheus health check falhou"
    fi
    
    print_check "Alertmanager Health"
    if response=$(curl -sf http://localhost:9093/-/healthy 2>&1); then
        print_success "Alertmanager health check OK"
    else
        print_fail "Alertmanager health check falhou"
    fi
    
    print_check "Webhook Receiver Health"
    if response=$(curl -sf http://localhost:5001/health 2>&1); then
        print_success "Webhook Receiver health check OK"
    else
        print_warning "Webhook Receiver health check falhou (opcional)"
    fi
    
    print_check "Producer Health"
    if response=$(curl -sf http://localhost:8081/actuator/health 2>&1); then
        status=$(echo "$response" | jq -r '.status' 2>/dev/null)
        if [ "$status" = "UP" ]; then
            print_success "Producer health: $status"
        else
            print_warning "Producer health: $status"
        fi
    else
        print_fail "Producer health check falhou"
    fi
    
    print_check "Consumer Health"
    if response=$(curl -sf http://localhost:8082/actuator/health 2>&1); then
        status=$(echo "$response" | jq -r '.status' 2>/dev/null)
        if [ "$status" = "UP" ]; then
            print_success "Consumer health: $status"
        else
            print_warning "Consumer health: $status"
        fi
    else
        print_fail "Consumer health check falhou"
    fi
}

validate_prometheus_targets() {
    print_section "4. PROMETHEUS TARGETS"
    
    print_check "Consultando targets do Prometheus"
    
    if targets=$(curl -sf http://localhost:9090/api/v1/targets 2>&1); then
        active_targets=$(echo "$targets" | jq -r '.data.activeTargets[] | "\(.labels.job):\(.health)"' 2>/dev/null)
        
        if [ -n "$active_targets" ]; then
            print_success "Targets ativos encontrados:"
            echo "$active_targets" | while read -r target; do
                job=$(echo "$target" | cut -d: -f1)
                health=$(echo "$target" | cut -d: -f2)
                
                if [ "$health" = "up" ]; then
                    print_success "  - $job: $health"
                else
                    print_warning "  - $job: $health"
                fi
            done
        else
            print_warning "Nenhum target ativo encontrado"
        fi
    else
        print_fail "Não foi possível consultar targets do Prometheus"
    fi
}

validate_alertmanager_integration() {
    print_section "5. INTEGRAÇÃO PROMETHEUS ↔ ALERTMANAGER"
    
    print_check "Verificando conexão Prometheus → Alertmanager"
    
    if alertmanagers=$(curl -sf http://localhost:9090/api/v1/alertmanagers 2>&1); then
        active_count=$(echo "$alertmanagers" | jq -r '.data.activeAlertmanagers | length' 2>/dev/null)
        
        if [ "$active_count" -gt 0 ]; then
            print_success "Prometheus conectado a $active_count Alertmanager(s)"
            
            echo "$alertmanagers" | jq -r '.data.activeAlertmanagers[].url' 2>/dev/null | while read -r url; do
                print_info "  - $url"
            done
        else
            print_fail "Prometheus NÃO está conectado ao Alertmanager"
        fi
    else
        print_fail "Não foi possível verificar integração"
    fi
}

validate_alert_rules() {
    print_section "6. REGRAS DE ALERTA"
    
    print_check "Consultando regras de alerta carregadas"
    
    if rules=$(curl -sf http://localhost:9090/api/v1/rules 2>&1); then
        groups=$(echo "$rules" | jq -r '.data.groups[]' 2>/dev/null)
        
        if [ -n "$groups" ]; then
            group_count=$(echo "$rules" | jq -r '.data.groups | length' 2>/dev/null)
            total_rules=$(echo "$rules" | jq -r '[.data.groups[].rules | length] | add' 2>/dev/null)
            
            print_success "Regras carregadas: $group_count grupos, $total_rules regras"
            
            echo "$rules" | jq -r '.data.groups[] | "\(.name):\(.rules | length)"' 2>/dev/null | while read -r group_info; do
                print_info "  - $group_info"
            done
            
            # Validar regras esperadas
            expected_groups=(
                "producer_critical_alerts"
                "consumer_critical_alerts"
                "kafka_performance_alerts"
                "infrastructure_alerts"
            )
            
            for expected_group in "${expected_groups[@]}"; do
                if echo "$rules" | jq -r '.data.groups[].name' | grep -q "$expected_group"; then
                    print_success "Grupo esperado encontrado: $expected_group"
                else
                    print_warning "Grupo esperado NÃO encontrado: $expected_group"
                fi
            done
        else
            print_fail "Nenhuma regra de alerta carregada"
        fi
    else
        print_fail "Não foi possível consultar regras de alerta"
    fi
}

validate_metrics_availability() {
    print_section "7. DISPONIBILIDADE DE MÉTRICAS"
    
    local critical_metrics=(
        "up"
        "events_published_total"
        "events_consumed_total"
        "validation_success_total"
        "validation_failure_total"
        "dlq_events_pending"
    )
    
    for metric in "${critical_metrics[@]}"; do
        print_check "Verificando métrica: $metric"
        
        if result=$(curl -sf "http://localhost:9090/api/v1/query?query=${metric}" 2>&1); then
            result_count=$(echo "$result" | jq -r '.data.result | length' 2>/dev/null)
            
            if [ "$result_count" -gt 0 ]; then
                print_success "Métrica '$metric' disponível ($result_count séries)"
            else
                print_warning "Métrica '$metric' não retornou dados"
            fi
        else
            print_fail "Erro ao consultar métrica '$metric'"
        fi
    done
}

validate_active_alerts() {
    print_section "8. ALERTAS ATIVOS"
    
    print_check "Consultando alertas atualmente ativos"
    
    if alerts=$(curl -sf http://localhost:9090/api/v1/alerts 2>&1); then
        firing_count=$(echo "$alerts" | jq -r '[.data.alerts[] | select(.state=="firing")] | length' 2>/dev/null)
        pending_count=$(echo "$alerts" | jq -r '[.data.alerts[] | select(.state=="pending")] | length' 2>/dev/null)
        
        if [ "$firing_count" -eq 0 ] && [ "$pending_count" -eq 0 ]; then
            print_success "Nenhum alerta ativo (sistema saudável)"
        else
            print_warning "$firing_count alerta(s) disparado(s), $pending_count pendente(s)"
            
            if [ "$firing_count" -gt 0 ]; then
                echo "$alerts" | jq -r '.data.alerts[] | select(.state=="firing") | .labels.alertname' 2>/dev/null | while read -r alert_name; do
                    print_info "  - FIRING: $alert_name"
                done
            fi
        fi
    else
        print_fail "Não foi possível consultar alertas ativos"
    fi
}

validate_alertmanager_config() {
    print_section "9. CONFIGURAÇÃO DO ALERTMANAGER"
    
    print_check "Verificando configuração do Alertmanager"
    
    if config=$(curl -sf http://localhost:9093/api/v2/status 2>&1); then
        print_success "Configuração do Alertmanager acessível"
        
        # Verificar receivers configurados
        if [ -f "config/alertmanager/config.yml" ]; then
            receiver_count=$(grep -c "^  - name:" config/alertmanager/config.yml || echo "0")
            print_info "Receivers configurados: $receiver_count"
        fi
    else
        print_fail "Não foi possível acessar configuração do Alertmanager"
    fi
}

test_alert_firing() {
    print_section "10. TESTE DE DISPARO DE ALERTAS"
    
    if [ "$MODE" != "full" ]; then
        print_info "Teste de disparo pulado (modo quick)"
        print_info "Execute com --full para testar disparo de alertas"
        return 0
    fi
    
    print_check "Teste: Disparando alerta sintético (parar Producer por 90s)"
    
    # Salvar estado atual
    initial_state=$(docker inspect -f '{{.State.Running}}' esocial-producer 2>/dev/null)
    
    if [ "$initial_state" != "true" ]; then
        print_warning "Producer já está parado, pulando teste"
        return 0
    fi
    
    # Parar Producer
    print_info "Parando Producer Service..."
    docker-compose stop producer-service > /dev/null 2>&1
    
    print_info "Aguardando 90 segundos para alerta disparar..."
    sleep 90
    
    # Verificar se alerta disparou
    if alerts=$(curl -sf http://localhost:9090/api/v1/alerts 2>&1); then
        if echo "$alerts" | jq -r '.data.alerts[].labels.alertname' | grep -q "ProducerServiceDown"; then
            print_success "Alerta 'ProducerServiceDown' disparou corretamente"
        else
            print_warning "Alerta 'ProducerServiceDown' NÃO disparou (pode levar mais tempo)"
        fi
    fi
    
    # Restaurar Producer
    print_info "Reiniciando Producer Service..."
    docker-compose start producer-service > /dev/null 2>&1
    
    print_info "Aguardando Producer voltar..."
    sleep 30
    
    print_success "Teste de disparo concluído"
}

test_webhook_notification() {
    print_section "11. TESTE DE NOTIFICAÇÃO (WEBHOOK)"
    
    print_check "Testando envio de notificação para webhook"
    
    test_payload='{
      "alerts": [
        {
          "status": "firing",
          "labels": {
            "alertname": "TestAlert",
            "severity": "info",
            "service": "validation-script"
          },
          "annotations": {
            "summary": "Teste de validação do sistema de alertas",
            "description": "Este é um alerta de teste gerado pelo script de validação"
          },
          "startsAt": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
        }
      ]
    }'
    
    if response=$(curl -sf -X POST http://localhost:5001/alerts \
        -H "Content-Type: application/json" \
        -d "$test_payload" 2>&1); then
        
        if echo "$response" | jq -r '.status' | grep -q "received"; then
            print_success "Webhook recebeu notificação de teste"
        else
            print_warning "Webhook respondeu, mas status inesperado"
        fi
    else
        print_warning "Webhook não respondeu (pode não estar configurado)"
    fi
}

validate_documentation() {
    print_section "12. DOCUMENTAÇÃO"
    
    local docs=(
        "docs/sprint3/ALERTING_SETUP.md"
        "docs/sprint3/ALERTING_RUNBOOKS.md"
        "config/prometheus/alerts.yml"
        "config/alertmanager/config.yml"
    )
    
    for doc in "${docs[@]}"; do
        if [ -f "$doc" ]; then
            print_success "Documentação encontrada: $doc"
        else
            print_warning "Documentação faltando: $doc"
        fi
    done
}

generate_report() {
    print_section "RESUMO DA VALIDAÇÃO"
    
    echo -e "\n${BOLD}Estatísticas:${NC}"
    echo "  Total de verificações: $TOTAL_CHECKS"
    echo -e "  ${GREEN}Passou: $PASSED_CHECKS${NC}"
    echo -e "  ${RED}Falhou: $FAILED_CHECKS${NC}"
    echo -e "  ${YELLOW}Avisos: $WARNING_CHECKS${NC}"
    
    # Calcular taxa de sucesso
    if [ "$TOTAL_CHECKS" -gt 0 ]; then
        success_rate=$(( (PASSED_CHECKS * 100) / TOTAL_CHECKS ))
        echo -e "\n${BOLD}Taxa de Sucesso: ${success_rate}%${NC}"
    fi
    
    echo -e "\n${BOLD}Status Geral:${NC}"
    if [ "$FAILED_CHECKS" -eq 0 ]; then
        echo -e "${GREEN}✅ Sistema de Alertas OPERACIONAL${NC}"
        exit_code=0
    elif [ "$FAILED_CHECKS" -le 2 ]; then
        echo -e "${YELLOW}⚠️  Sistema PARCIALMENTE operacional (revisar falhas)${NC}"
        exit_code=1
    else
        echo -e "${RED}❌ Sistema com PROBLEMAS CRÍTICOS${NC}"
        exit_code=2
    fi
    
    echo -e "\n${BOLD}Recomendações:${NC}"
    if [ "$FAILED_CHECKS" -gt 0 ]; then
        echo "  - Revisar logs dos containers com falhas"
        echo "  - Verificar configurações em config/"
        echo "  - Consultar documentação: docs/sprint3/ALERTING_SETUP.md"
    fi
    
    if [ "$WARNING_CHECKS" -gt 0 ]; then
        echo "  - Revisar avisos para otimizar sistema"
    fi
    
    echo -e "\n${BOLD}Próximos Passos:${NC}"
    if [ "$MODE" = "quick" ]; then
        echo "  - Execute com --full para validação completa com testes de disparo"
    fi
    echo "  - Configurar notificações (email/Slack) em config/alertmanager/config.yml"
    echo "  - Criar runbooks para alertas críticos"
    echo "  - Treinar equipe no uso do sistema"
    
    echo -e "\n${CYAN}Log salvo em: $LOG_FILE${NC}"
    
    return $exit_code
}

#######################################################################
# EXECUÇÃO PRINCIPAL
#######################################################################

main() {
    # Iniciar log
    exec > >(tee -a "$LOG_FILE")
    exec 2>&1
    
    print_header "VALIDAÇÃO DO SISTEMA DE ALERTAS - Pipeline ETL eSocial"
    
    echo -e "${BOLD}Data:${NC} $(date)"
    echo -e "${BOLD}Modo:${NC} $MODE"
    echo -e "${BOLD}Diretório:${NC} $(pwd)"
    echo ""
    
    # Executar validações
    validate_prerequisites
    validate_containers
    validate_health_checks
    validate_prometheus_targets
    validate_alertmanager_integration
    validate_alert_rules
    validate_metrics_availability
    validate_active_alerts
    validate_alertmanager_config
    
    # Testes avançados (apenas em modo full)
    if [ "$MODE" = "full" ]; then
        test_alert_firing
    fi
    
    test_webhook_notification
    validate_documentation
    
    # Gerar relatório final
    generate_report
}

# Executar
main "$@"
