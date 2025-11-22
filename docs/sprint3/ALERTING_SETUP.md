# Sistema de Alertas - Guia de Setup Completo

**Projeto:** Pipeline ETL eSocial com Apache Kafka  
**Sprint:** 3 - Card 3.6  
**Versão:** 1.0  
**Data:** 2025-11-22

---

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Arquitetura do Sistema de Alertas](#arquitetura-do-sistema-de-alertas)
3. [Pré-requisitos](#pré-requisitos)
4. [Instalação e Configuração](#instalação-e-configuração)
5. [Validação da Instalação](#validação-da-instalação)
6. [Catálogo de Alertas](#catálogo-de-alertas)
7. [Configuração de Notificações](#configuração-de-notificações)
8. [Testes de Alertas](#testes-de-alertas)
9. [Troubleshooting](#troubleshooting)
10. [Manutenção](#manutenção)

---

## 🎯 Visão Geral

O sistema de alertas do Pipeline eSocial monitora automaticamente a saúde e performance dos componentes, notificando a equipe sobre problemas críticos antes que impactem a operação.

### Objetivos

- ✅ **Detecção proativa** de problemas
- ✅ **Notificação automática** da equipe responsável
- ✅ **Redução do MTTR** (Mean Time To Recover)
- ✅ **Visibilidade** do estado do sistema 24/7

### Componentes

| Componente | Função | Porta |
|------------|--------|-------|
| **Prometheus** | Coleta métricas e avalia regras de alerta | 9090 |
| **Alertmanager** | Gerencia e roteia alertas | 9093 |
| **Webhook Receiver** | Recebe notificações (dev/test) | 5001 |
| **Grafana** | Visualização de alertas (opcional) | 3000 |

---

## 🏗️ Arquitetura do Sistema de Alertas
```
┌─────────────────┐
│ Producer Service│
│ Consumer Service│──┐
└─────────────────┘  │
                     │ Métricas (/actuator/prometheus)
                     ▼
            ┌──────────────┐
            │ PROMETHEUS   │
            │ (Scraper)    │
            └──────┬───────┘
                   │
                   │ Avalia Regras (alerts.yml)
                   ▼
            ┌──────────────┐
            │ ALERTMANAGER │
            │ (Router)     │    
            └──────┬───────┘
                   │
      ┌────────────┼────────────┐
      ▼            ▼            ▼
    ┌────────┐ ┌─────────┐ ┌──────────┐
    │ Email  │ │ Slack   │ │ Webhook  │
    └────────┘ └─────────┘ └──────────┘
```

### Fluxo de Alertas

1. **Coleta**: Prometheus faz scraping das métricas dos serviços a cada 15s
2. **Avaliação**: Prometheus avalia regras de alerta a cada 15s
3. **Disparo**: Quando condição é atendida por tempo definido (`for`), alerta é disparado
4. **Agrupamento**: Alertmanager agrupa alertas similares (evita spam)
5. **Roteamento**: Alertas são enviados para receptores conforme severidade
6. **Notificação**: Equipe recebe notificação via email/Slack/webhook

---

## 📋 Pré-requisitos

### Obrigatórios

- ✅ Docker e Docker Compose instalados
- ✅ Pipeline eSocial rodando (Producer + Consumer + Kafka + PostgreSQL)
- ✅ Prometheus configurado e coletando métricas

### Validar Pré-requisitos
1. Verificar serviços básicos
```
docker-compose ps | grep -E "(prometheus|producer|consumer)"
```
2. Verificar métricas disponíveis
```
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'
```
3. Verificar scrape do Producer
```
curl -s http://localhost:8081/actuator/prometheus | grep events_published_total
```
4. Verificar scrape do Consumer
```
curl -s http://localhost:8082/actuator/prometheus | grep events_consumed_total
```

**Resultado esperado:** Todos os serviços `UP` e métricas disponíveis.

---

## 🚀 Instalação e Configuração

### Passo 1: Estrutura de Diretórios
Na raiz do projeto etl-kafka-esocial
```
mkdir -p config/alertmanager
mkdir -p config/alertmanager/templates
mkdir -p scripts/webhook
mkdir -p docs/sprint3/runbooks
```

### Passo 2: Arquivos de Configuração

#### 2.1 Alertmanager Config (`config/alertmanager/config.yml`)
```
# Configuração do Alertmanager para Sistema eSocial ETL

global:
  resolve_timeout: 5m

# Templates customizados
templates:
  - '/etc/alertmanager/templates/*.tmpl'

# Roteamento de alertas
route:
  receiver: 'default'
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s        # Aguarda antes de enviar primeiro alerta
  group_interval: 10s    # Intervalo entre alertas do mesmo grupo
  repeat_interval: 12h   # Repete alerta se não resolvido
  
  routes:
    # Alertas críticos - notificação imediata
    - match:
        severity: critical
      receiver: 'critical-alerts'
      group_wait: 5s
      repeat_interval: 4h
      continue: true
    
    # Alertas de warning - agrupados
    - match:
        severity: warning
      receiver: 'warning-alerts'
      group_wait: 30s
      repeat_interval: 12h

# Receptores de notificação
receivers:
  # Receptor padrão - webhook para testes
  - name: 'default'
    webhook_configs:
      - url: 'http://webhook-receiver:5001/alerts'
        send_resolved: true
        max_alerts: 0

  # Receptor para alertas críticos
  - name: 'critical-alerts'
    webhook_configs:
      - url: 'http://webhook-receiver:5001/alerts/critical'
        send_resolved: true

  # Receptor para alertas de warning
  - name: 'warning-alerts'
    webhook_configs:
      - url: 'http://webhook-receiver:5001/alerts/warning'
        send_resolved: true

# Inibição de alertas (evitar spam)
inhibit_rules:
  # Se um alerta crítico está ativo, não envia warnings relacionados
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'service']
  
  # Se serviço está down, não alerta sobre outras métricas dele
  - source_match:
      alertname: 'ServiceDown'
    target_match_re:
      alertname: '(HighErrorRate|HighLatency|HighConsumerLag)'
    equal: ['service']
```
#### 2.2 Atualizar Prometheus (`config/prometheus/prometheus.yml`)

Adicionar ao arquivo existente:
```
# Alertmanager
alerting:
    alertmanagers:
        - static_configs:
        - targets:
        - alertmanager:9093
    timeout: 10s

# Regras de alerta
rule_files:
    'alerts.yml'
```

#### 2.3 Regras de Alerta

O arquivo `config/prometheus/alerts.yml` já foi consolidado na resposta anterior.

#### 2.4 Webhook Receiver (Opcional - para testes)

**Arquivo:** `scripts/webhook/app.py`
```
#!/usr/bin/env python3
from flask import Flask, request, jsonify
from datetime import datetime
import logging

app = Flask(name)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(name)

def format_alert(alert):
status = alert.get('status', 'unknown').upper()
labels = alert.get('labels', {})
annotations = alert.get('annotations', {})

severity = labels.get('severity', 'info')
emoji_map = {'critical': '🚨', 'warning': '⚠️', 'info': 'ℹ️'}
emoji = emoji_map.get(severity, '📢')

output = [
    f"\n{'='*80}",
    f"{emoji} ALERTA {status} - {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}",
    f"{'='*80}",
    f"\n📊 Nome: {labels.get('alertname', 'N/A')}",
    f"⚠️  Severidade: {severity.upper()}",
    f"🔧 Serviço: {labels.get('service', 'N/A')}",
    f"🏷️  Componente: {labels.get('component', 'N/A')}",
    f"📝 Descrição:\n{annotations.get('description', 'N/A')}",
]

if annotations.get('action'):
    output.append(f"\n💡 Ações Recomendadas:\n{annotations['action']}")

output.append(f"\n⏰ Início: {alert.get('startsAt', 'N/A')}")
output.append(f"{'='*80}\n")

return '\n'.join(output)

@app.route('/health', methods=['GET'])
def health():
return jsonify({"status": "healthy"}), 200

@app.route('/alerts', methods=['POST'])
@app.route('/alerts/path:subpath', methods=['POST'])
def receive_alerts(subpath=None):
try:
alert_data = request.json
alerts = alert_data.get('alerts', [])

    logger.info(f"[{subpath or 'default'}] Recebidos {len(alerts)} alerta(s)")
    
    for alert in alerts:
        print(format_alert(alert))
    
    return jsonify({
        "status": "received",
        "count": len(alerts),
        "timestamp": datetime.now().isoformat()
    }), 200

except Exception as e:
    logger.error(f"Erro: {str(e)}")
    return jsonify({"error": str(e)}), 500

if name == 'main':
print("="*80)
print("🚀 Webhook Receiver - Pipeline eSocial")
print("="*80)
print("📡 Porta: 5001")
print("🔗 Endpoints: POST /alerts, /alerts/critical, /alerts/warning")
print("="*80 + "\n")
app.run(host='0.0.0.0', port=5001, debug=False)
```

**Arquivo:** `scripts/webhook/Dockerfile`
```
FROM python:3.11-slim
WORKDIR /app
RUN pip install --no-cache-dir flask==3.0.0
COPY app.py .
EXPOSE 5001
CMD ["python", "app.py"]
```
### Passo 3: Atualizar Docker Compose

Adicionar ao `docker-compose.yml`:
```
volumes:

#Adicionar ao final da seção volumes
alertmanager-data:
    services:

#Adicionar após prometheus
alertmanager:
    image: prom/alertmanager:v0.26.0
    container_name: esocial-alertmanager
    hostname: alertmanager
    networks:
        - esocial-network
    ports:
        - "9093:9093"
    command:
        - '--config.file=/etc/alertmanager/config.yml'
        - '--storage.path=/alertmanager'
        - '--web.external-url=http://localhost:9093'
    volumes:
        - ./config/alertmanager/config.yml:/etc/alertmanager/config.yml
        - alertmanager-data:/alertmanager
    healthcheck:
    test: ["CMD", "wget", "--spider", "-q", "http://localhost:9093/-/healthy"]
    interval: 10s
    timeout: 5s
    retries: 3
    restart: unless-stopped

webhook-receiver:
    build:
    context: ./scripts/webhook
    dockerfile: Dockerfile
    image: esocial-webhook:latest
    container_name: esocial-webhook-receiver
    hostname: webhook-receiver
    networks:
        - esocial-network
    ports:
        - "5001:5001"
    restart: unless-stopped
```

### Passo 4: Deploy
#### 1. Parar stack atual (se necessário)
```
docker-compose down
```
#### 2. Build do webhook receiver
```
docker-compose build webhook-receiver
```
#### 3. Iniciar todos os serviços
```
docker-compose up -d
```
#### 4. Aguardar containers ficarem healthy
```
sleep 30
```
#### 5. Verificar status
```
docker-compose ps
```
---

## ✅ Validação da Instalação

### 1. Verificar Containers
#### Verificar se todos estão rodando
```
docker-compose ps | grep -E "(alertmanager|webhook-receiver|prometheus)"
```
Resultado esperado:
```
esocial-alertmanager Up (healthy)
esocial-webhook-receiver Up
esocial-prometheus Up
```

### 2. Verificar Health Checks
#### Alertmanager
```
curl -s http://localhost:9093/-/healthy
```
Esperado: Alertmanager is Healthy.
#### Webhook Receiver
```
curl -s http://localhost:5001/health | jq
```
Esperado: {"status": "healthy"}
#### Prometheus
```
curl -s http://localhost:9090/-/healthy
```
Esperado: Prometheus is Healthy.

### 3. Verificar Integração Prometheus ↔ Alertmanager
#### Listar Alertmanagers conectados ao Prometheus
```
curl -s http://localhost:9090/api/v1/alertmanagers | jq '.data.activeAlertmanagers'
```
Esperado:
```
{
"url": "http://alertmanager:9093/api/v2/alerts"
}
```

### 4. Verificar Regras de Alerta Carregadas
#### Listar grupos de regras
```
curl -s http://localhost:9090/api/v1/rules | jq '.data.groups[] | {name: .name, rules: (.rules | length)}'
```
Esperado:
```
{"name": "producer_critical_alerts", "rules": 4}
{"name": "consumer_critical_alerts", "rules": 6}
{"name": "kafka_performance_alerts", "rules": 2}
{"name": "infrastructure_alerts", "rules": 3}
```

### 5. Acessar Interfaces Web

#### Abrir Alertmanager UI
```
open http://localhost:9093
```
#### Abrir Prometheus Alerts
```
open http://localhost:9090/alerts
```
#### Ver targets
```
open http://localhost:9090/targets
```

---

## 📊 Catálogo de Alertas

### Resumo por Severidade

| Severidade | Quantidade | Tempo de Resposta |
|------------|------------|-------------------|
| **Critical** | 8 | < 15 minutos |
| **Warning** | 7 | < 1 hora |
| **Info** | 1 | < 24 horas |

### Alertas Críticos (Requerem Ação Imediata)

| Alerta | Componente | Condição | For | Ação |
|--------|------------|----------|-----|------|
| **ProducerServiceDown** | Producer | Service DOWN | 1m | Reiniciar serviço |
| **ConsumerServiceDown** | Consumer | Service DOWN | 1m | Reiniciar serviço |
| **ProducerHighErrorRate** | Producer | Erro > 5% | 5m | Analisar logs |
| **ConsumerHighValidationErrorRate** | Consumer | Validação falha > 5% | 5m | Verificar dados |
| **DLQCritical** | Consumer DLQ | Eventos > 500 | 5m | Reprocessar DLQ |
| **KafkaBrokerDown** | Kafka | Broker DOWN | 2m | Reiniciar broker |
| **PostgreSQLDown** | Database | PostgreSQL DOWN | 1m | Reiniciar DB |

### Alertas de Warning

| Alerta | Componente | Condição | For | Ação |
|--------|------------|----------|-----|------|
| **CDCHighLatency** | Producer CDC | P95 > 10s | 5m | Otimizar queries |
| **DLQAccumulating** | Consumer DLQ | Eventos > 100 | 10m | Monitorar tendência |
| **ValidationLatencyHigh** | Consumer | P95 > 5s | 5m | Otimizar validação |
| **NoEventsProcessed** | Pipeline | Rate = 0 | 10m | Verificar origem |
| **KafkaPublishLatencyHigh** | Kafka | P95 > 1s | 5m | Verificar Kafka |
| **HighMemoryUsage** | JVM | Heap > 85% | 5m | Aumentar memória |
| **ProducerLowThroughput** | Producer | < 1 evt/min | 10m | Verificar origem |

### Alertas Informativos

| Alerta | Componente | Condição | For | Ação |
|--------|------------|----------|-----|------|
| **HighPayloadSize** | Kafka | P95 > 10KB | 10m | Analisar estrutura |

---

## 🔔 Configuração de Notificações

### Email (SMTP)

Editar `config/alertmanager/config.yml`:
```
global:
smtp_smarthost: 'smtp.gmail.com:587'
smtp_from: 'alertas-esocial@empresa.com'
smtp_auth_username: 'seu-email@gmail.com'
smtp_auth_password: 'sua-senha-app' # Criar senha de app no Gmail
smtp_require_tls: true

receivers:

name: 'critical-alerts'
email_configs:

to: 'admin@empresa.com,suporte@empresa.com'
subject: '🚨 [CRÍTICO] {{ .GroupLabels.alertname }} - Pipeline eSocial'
html: |

<h2>🚨 Alerta Crítico</h2> <p><strong>Alerta:</strong> {{ .GroupLabels.alertname }}</p> <p><strong>Serviço:</strong> {{ .GroupLabels.service }}</p> <p><strong>Descrição:</strong> {{ .CommonAnnotations.description }}</p> <p><strong>Horário:</strong> {{ .StartsAt.Format "02/01/2006 15:04:05" }}</p>
```

**Recarregar configuração:**
```
docker-compose restart alertmanager
```
### Slack

1. **Criar Incoming Webhook** no Slack:
   - Acessar: https://api.slack.com/messaging/webhooks
   - Criar app e ativar Incoming Webhooks
   - Copiar URL do webhook

2. **Configurar em `config/alertmanager/config.yml`:**

```

receivers:

- name: 'critical-alerts'
slack_configs:
    - api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
channel: '\#alerts-esocial-critical'
title: '🚨 [CRÍTICO] {{ .GroupLabels.alertname }}'
text: |
*Serviço:* {{ .GroupLabels.service }}
*Componente:* {{ .GroupLabels.component }}
*Descrição:* {{ .CommonAnnotations.description }}
*Horário:* {{ .StartsAt.Format "02/01/2006 15:04:05" }}
send_resolved: true

```

3. **Recarregar:**

```

docker-compose restart alertmanager

```

### PagerDuty (Opcional)

```

receivers:

- name: 'critical-alerts'
pagerduty_configs:
    - service_key: 'YOUR_PAGERDUTY_SERVICE_KEY'
description: '{{ .GroupLabels.alertname }} - {{ .CommonAnnotations.summary }}'

```

---

## 🧪 Testes de Alertas

### Script de Teste Automatizado

**Criar:** `scripts/test-alerts.sh`

```

\#!/bin/bash

set -e

echo "🧪 Testando Sistema de Alertas"
echo "================================"

# Teste 1: Injetar eventos inválidos (HighValidationErrorRate)

echo -e "\n1️⃣ Teste: Gerar erros de validação"
for i in {1..30}; do
docker exec esocial-postgres-db psql -U esocial_user -d esocial -c "
INSERT INTO source.employees VALUES (
'TEST_ERR_\$i',
'123',  -- CPF inválido
NULL,
'Teste Erro \$i',
'2030-01-01',  -- Data futura (inválida)
'2024-01-01',
NULL,
'Teste',
'TI',
500.00,  -- Salário muito baixo
'ACTIVE',
NOW(),
NOW()
);
" > /dev/null 2>\&1
done
echo "✅ 30 eventos inválidos injetados"
echo "⏳ Aguarde 5 minutos e verifique: http://localhost:9090/alerts"

# Teste 2: Simular Consumer Lag

echo -e "\n2️⃣ Teste: Simular consumer lag (pausar consumer)"
docker-compose pause consumer-service
echo "⏸️  Consumer pausado por 2 minutos..."
sleep 120
docker-compose unpause consumer-service
echo "▶️  Consumer retomado"

# Teste 3: Simular Service Down

echo -e "\n3️⃣ Teste: Simular serviço down"
docker-compose stop producer-service
echo "🛑 Producer parado por 90 segundos..."
sleep 90
docker-compose start producer-service
echo "🚀 Producer reiniciado"

echo -e "\n================================"
echo "✅ Testes concluídos!"
echo "📊 Verificar alertas:"
echo "   - Prometheus: http://localhost:9090/alerts"
echo "   - Alertmanager: http://localhost:9093"
echo "   - Webhook Logs: docker logs esocial-webhook-receiver -f"

```

**Executar:**

```

chmod +x scripts/test-alerts.sh
./scripts/test-alerts.sh

```

### Teste Manual de Alerta Específico

#### Testar ProducerServiceDown

```


# Parar Producer

docker-compose stop producer-service

# Aguardar 1 minuto (for: 1m)

# Verificar alerta disparou

curl -s http://localhost:9090/api/v1/alerts | jq '.data.alerts[] | select(.labels.alertname=="ProducerServiceDown")'

# Reiniciar Producer

docker-compose start producer-service

# Alerta deve resolver automaticamente

```

#### Testar DLQAccumulating

```


# Inserir 150 eventos inválidos

for i in {1..150}; do
docker exec esocial-postgres-db psql -U esocial_user -d esocial -c "
INSERT INTO source.employees (employee_id, cpf, name, birth_date, hire_date, position, department, salary, status, created_at, updated_at)
VALUES ('TEST_DLQ_\$i', '123', 'Teste DLQ', '2030-01-01', '2024-01-01', 'Teste', 'TI', 100, 'ACTIVE', NOW(), NOW());
" > /dev/null 2>\&1
done

# Aguardar processamento (5 minutos)

# Verificar DLQ via API

curl -s http://localhost:8082/api/v1/validation/dlq | jq 'length'

# Verificar alerta

curl -s http://localhost:9090/api/v1/alerts | jq '.data.alerts[] | select(.labels.alertname=="DLQAccumulating")'

```

---

## 🔧 Troubleshooting

### Problema: Alertas não estão sendo disparados

**Diagnóstico:**

```


# 1. Verificar se regras foram carregadas

curl -s http://localhost:9090/api/v1/rules | jq

# 2. Verificar se métricas estão disponíveis

curl -s http://localhost:9090/api/v1/query?query=up | jq

# 3. Ver logs do Prometheus

docker logs esocial-prometheus --tail 100 | grep -i "error\|warning"

```

**Solução:**

```


# Recarregar configuração do Prometheus

curl -X POST http://localhost:9090/-/reload

# Ou reiniciar container

docker-compose restart prometheus

```

### Problema: Alertmanager não recebe alertas

**Diagnóstico:**

```


# Verificar conexão Prometheus → Alertmanager

curl -s http://localhost:9090/api/v1/alertmanagers | jq

# Ver logs do Alertmanager

docker logs esocial-alertmanager --tail 100

```

**Solução:**

```


# Verificar configuração do Prometheus

cat config/prometheus/prometheus.yml | grep -A 5 "alerting:"

# Reiniciar Alertmanager

docker-compose restart alertmanager

```

### Problema: Webhook não recebe notificações

**Diagnóstico:**

```


# Verificar logs do webhook

docker logs esocial-webhook-receiver -f

# Testar webhook manualmente

curl -X POST http://localhost:5001/alerts \
-H "Content-Type: application/json" \
-d '{"alerts": [{"status": "firing", "labels": {"alertname": "Test"}, "annotations": {"description": "Teste manual"}}]}'

```

**Solução:**

```


# Verificar conectividade Alertmanager → Webhook

docker exec esocial-alertmanager wget -O- http://webhook-receiver:5001/health

# Reiniciar webhook

docker-compose restart webhook-receiver

```

### Problema: Email não está sendo enviado

**Diagnóstico:**

```


# Ver logs do Alertmanager

docker logs esocial-alertmanager | grep -i "email\|smtp"

```

**Soluções Comuns:**

1. **Gmail bloqueando:** Ativar "Acesso a apps menos seguros" ou criar "Senha de app"
2. **Firewall:** Verificar se porta 587 está aberta
3. **Credenciais:** Validar usuário/senha SMTP

---

## 🔧 Manutenção

### Atualizar Regras de Alerta

```


# 1. Editar arquivo

vim config/prometheus/alerts.yml

# 2. Validar sintaxe

docker run --rm -v \$(pwd)/config/prometheus:/prometheus prom/prometheus:latest promtool check rules /prometheus/alerts.yml

# 3. Recarregar sem downtime

curl -X POST http://localhost:9090/-/reload

```

### Atualizar Configuração do Alertmanager

```


# 1. Editar arquivo

vim config/alertmanager/config.yml

# 2. Validar sintaxe

docker exec esocial-alertmanager amtool check-config /etc/alertmanager/config.yml

# 3. Recarregar

docker exec esocial-alertmanager kill -HUP 1

```

### Silenciar Alerta Temporariamente

```


# Via API (silenciar por 2 horas)

curl -X POST http://localhost:9093/api/v2/silences \
-H "Content-Type: application/json" \
-d '{
"matchers": [
{"name": "alertname", "value": "ProducerServiceDown", "isRegex": false}
],
"startsAt": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'",
    "endsAt": "'$(date -u -d '+2 hours' +%Y-%m-%dT%H:%M:%S.%3NZ)'",
"createdBy": "admin",
"comment": "Manutenção programada"
}'

# Via UI: http://localhost:9093/\#/silences

```

### Backup de Dados do Alertmanager

```


# Backup do volume

docker run --rm \
-v esocial_alertmanager-data:/data \
-v $(pwd)/backups:/backup \
  alpine tar czf /backup/alertmanager-backup-$(date +%Y%m%d).tar.gz /data

```

