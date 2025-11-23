# Treinamento: Pipeline ETL eSocial - Equipe de Operações

**Versão:** 1.0  
**Data:** 2025-11-22  
**Duração:** 4 horas  
**Público-alvo:** Equipe de Sustentação e Operações  
**Instrutor:** Márcio Kuroki Gonçalves

---

## 📋 Agenda do Treinamento

| Horário | Duração | Módulo | Formato |
|---------|---------|--------|---------|
| 09:00 - 09:45 | 45 min | **Módulo 1:** Visão Geral da Arquitetura | Apresentação |
| 09:45 - 10:30 | 45 min | **Módulo 2:** Demonstração Hands-On | Prática |
| 10:30 - 10:45 | 15 min | ☕ **Coffee Break** | - |
| 10:45 - 11:30 | 45 min | **Módulo 3:** Monitoramento e Dashboards | Hands-On |
| 11:30 - 12:30 | 60 min | **Módulo 4:** Troubleshooting Simulado | Prática |
| 12:30 - 13:00 | 30 min | **Q&A e Feedback** | Discussão |

---

## Módulo 1: Visão Geral da Arquitetura (45 min)

### Slide 1: Título

```

╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║      PIPELINE ETL eSocial com Apache Kafka                ║
║                                                           ║
║           Treinamento para Operações                      ║
║                                                           ║
║          Márcio Kuroki Gonçalves - 2025                   ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝

```

---

### Slide 2: Por Que Este Projeto?

**Problema Atual (Sistema Legado):**
- ❌ Processamento batch (1x por dia)
- ❌ Latência alta (24 horas)
- ❌ Sem visibilidade de erros
- ❌ Difícil manutenção
- ❌ Baixa disponibilidade (98%)

**Nova Solução (Pipeline Kafka):**
- ✅ Processamento em tempo real (< 5 segundos)
- ✅ Validações automáticas (11 regras)
- ✅ Dashboard de monitoramento
- ✅ Arquitetura escalável
- ✅ Alta disponibilidade (99.7%+)

---

### Slide 3: Arquitetura de Alto Nível

```

┌─────────────────────────────────────────────────────────────┐
│                    FLUXO DE DADOS                           │
└─────────────────────────────────────────────────────────────┘

Sistema RH                Producer              Kafka
(PostgreSQL)              Service               Cluster
┌──────────┐            ┌──────────┐         ┌──────────┐
│          │            │          │         │ Broker 1 │
│ Employees│            │   CDC    │         │ Broker 2 │
│  Table   │────5s────> | Polling  │────────>│ Broker 3 │
│          │            │          │         │          │
└──────────┘            └──────────┘         └────┬─────┘
                                                  │
                                                  ▼
PostgreSQL              Consumer              Validações
(Destino)               Service               Motor
┌──────────┐            ┌──────────┐         ┌──────────┐
│          │            │          │         │ 11 Regras│
│ Validated│<───────────│ Persist  │<────────│ Estrut + │
│   Data   │            │          │         │ Negócio  │
└──────────┘            └──────────┘         └──────────┘
    │                       │
    │                       ▼
    │                  ┌──────────┐
    │                  │   DLQ    │
    │                  │ (Erros)  │
    └─────────────────>│          │
                       └──────────┘

```

---

### Slide 4: Componentes Principais

| Componente | Porta | O Que Faz | Seu Papel |
|------------|-------|-----------|-----------|
| **Producer Service** | 8081 | Captura mudanças no banco origem | Monitorar health |
| **Consumer Service** | 8082 | Valida e persiste dados | Monitorar latência e erros |
| **Kafka Cluster** | 9092-9094 | Transporta mensagens | Monitorar lag e throughput |
| **PostgreSQL** | 5432 | Armazena dados | Monitorar espaço e conexões |
| **Grafana** | 3000 | Dashboards visuais | Acompanhar métricas |
| **Prometheus** | 9090 | Coleta métricas | Verificar targets UP |
| **Kafka UI** | 8090 | Interface Kafka | Inspecionar mensagens |

---

### Slide 5: Fluxo de Processamento Detalhado

**1. CDC (Change Data Capture) - Producer**
```

A cada 5 segundos:

1. Query no PostgreSQL (source.employees)
2. Detecta mudanças (INSERT/UPDATE/DELETE)
3. Cria evento JSON (EmployeeEventDTO)
4. Publica no tópico Kafka correto
    - employee-create
    - employee-update
    - employee-delete
```

**2. Validação - Consumer**
```

Ao receber evento:

1. Consume do Kafka
2. Executa 11 validações (fail-fast)
    - 6 estruturais (CPF, PIS, datas...)
    - 5 de negócio (idade mínima, salário...)
3. SE válido → persiste
4. SE inválido → envia para DLQ
```

**3. Persistência e Audit**
```

Dados válidos:

1. Salva em public.employees
2. Incrementa version (optimistic lock)
3. Trigger cria audit trail
4. Retorna sucesso
```

---

### Slide 6: Conceitos Importantes

**1. Event-Driven Architecture (EDA)**
- Sistema reage a eventos (mudanças) em tempo real
- Componentes desacoplados (independentes)
- Alta escalabilidade

**2. Dead Letter Queue (DLQ)**
- "Fila de erros" para eventos inválidos
- Permite reprocessamento manual
- Evita perda de dados

**3. Optimistic Locking (Versão)**
- Cada alteração incrementa `version`
- Previne concorrência (2 usuários alterando simultaneamente)
- Histórico completo em `audit.employees_history`

**4. Fail-Fast**
- Para no primeiro erro crítico
- Economiza processamento
- Feedback rápido

---

### Slide 7: Seu Papel como Operador

**Responsabilidades Diárias:**

✅ **Monitoramento**
- Verificar dashboards Grafana 3x/dia
- Alertas Slack/Email (responder em 15 min)
- Health checks (Producer e Consumer UP?)

✅ **Investigação**
- Consumer lag alto? Verificar causa
- Taxa de erro elevada? Analisar DLQ
- Performance degradada? Checar recursos

✅ **Ação Corretiva**
- Restart de serviços (se necessário)
- Reprocessamento de eventos DLQ
- Escalação para dev (se bug no código)

❌ **NÃO É SUA RESPONSABILIDADE:**
- Corrigir bugs no código (escalar para dev)
- Alterar infraestrutura Kafka/PostgreSQL (DBA)
- Implementar novas validações (dev)

---

### Slide 8: Métricas-Chave (Memorize!)

| Métrica | Normal | Alerta | Crítico | Ação |
|---------|--------|--------|---------|------|
| **Throughput** | 800-1.500/s | < 500/s | < 200/s | Investigar gargalo |
| **Latência P95** | 50-100ms | 200ms | > 500ms | Otimizar queries |
| **Taxa de Erro** | 5-10% | 15% | > 20% | Analisar DLQ |
| **Consumer Lag** | < 100 | 500 | > 1.000 | Escalar consumer |
| **DLQ Eventos** | < 50 | 200 | > 1.000 | Reprocessar urgente |
| **Uptime** | > 99.7% | < 99% | < 95% | Incidente crítico |

---

## Módulo 2: Demonstração Hands-On (45 min)

### Exercício 1: Acessar Ferramentas de Monitoramento

**Objetivo:** Familiarizar com as interfaces principais.

#### Passo 1: Acessar Grafana

```


# Abrir navegador

http://localhost:3000

# Login

Usuário: admin
Senha: admin

# Navegar

Dashboards → Browse → Overview Geral

```

**O que observar:**
- Total de eventos processados (último hora)
- Taxa de sucesso vs erro (%)
- Latência P95 (ms)
- Gráfico de throughput (linha temporal)

---

#### Passo 2: Acessar Prometheus

```


# Abrir navegador

http://localhost:9090

# Testar query

events_published_total

# Ver resultado

Graph → Execute → Visualizar gráfico

```

---

#### Passo 3: Acessar Kafka UI

```


# Abrir navegador

http://localhost:8090

# Explorar

Topics → employee-create → Messages → Ver últimas mensagens
Consumer Groups → esocial-consumer-group → Ver lag

```

---

### Exercício 2: Inserir Dados e Acompanhar Processamento

**Objetivo:** Ver o pipeline funcionando end-to-end.

#### Passo 1: Conectar no PostgreSQL (Origem)

```


# Terminal

docker exec -it esocial-postgres-db psql -U esocial_user -d esocial

```

#### Passo 2: Inserir Colaborador

```

-- Inserir
INSERT INTO source.employees VALUES (
'EMP999',
'12345678901',
'10011223344',
'Maria Silva Operadora',
'1990-05-20',
'2024-01-15',
NULL,
'Analista de Suporte',
'TI',
5500.00,
'ACTIVE',
NOW(),
NOW()
);

-- Verificar inserção
SELECT * FROM source.employees WHERE employee_id = 'EMP999';

```

#### Passo 3: Aguardar CDC (5 segundos)

```


# Contar até 5...

echo "Aguardando CDC processar... 5 segundos"
sleep 5

```

#### Passo 4: Verificar em Kafka UI

```

1. Abrir http://localhost:8090
2. Topics → employee-create
3. Messages → Procurar por "EMP999"
4. Verificar JSON do evento
```

#### Passo 5: Verificar Processamento (PostgreSQL Destino)

```

-- Verificar dados processados
SELECT source_id, full_name, version FROM public.employees
WHERE source_id = 'EMP999';

-- Resultado esperado:
-- source_id | full_name               | version
-- EMP999    | Maria Silva Operadora   | 1

-- Verificar audit trail
SELECT operation, full_name, version, changed_at
FROM audit.employees_history
WHERE source_id = 'EMP999'
ORDER BY changed_at DESC;

-- Resultado esperado:
-- operation | full_name               | version | changed_at
-- INSERT    | Maria Silva Operadora   | 1       | 2025-11-22 10:15:32

```

#### Passo 6: Verificar no Grafana

```

1. Abrir Grafana → Dashboard Overview
2. Ver "Total de Eventos" incrementou (+1)
3. Ver "Taxa de Sucesso" manteve 100%
4. Ver gráfico de linha com spike no momento da inserção
```

✅ **Sucesso!** Você acompanhou um evento do início ao fim.

---

### Exercício 3: Consultar Erros e DLQ

**Objetivo:** Aprender a identificar e analisar erros.

#### Passo 1: Inserir Dado Inválido (CPF errado)

```

-- Inserir com CPF inválido (9 dígitos ao invés de 11)
INSERT INTO source.employees VALUES (
'EMP998',
'123456789',  -- ← CPF inválido!
'10011223344',
'João Erro',
'1990-01-01',
'2024-01-01',
NULL,
'Testador',
'QA',
5000.00,
'ACTIVE',
NOW(),
NOW()
);

```

#### Passo 2: Aguardar Processamento (5 segundos)

```

sleep 5

```

#### Passo 3: Verificar Erro na API REST

```


# Consultar erros de validação

curl http://localhost:8082/api/v1/validation/errors | jq '.[] | select(.sourceId == "EMP998")'

# Resultado esperado:

# {

# "id": 1,

# "sourceId": "EMP998",

# "ruleName": "INVALID_CPF_FORMAT",

# "errorMessage": "CPF '123456789' deve ter 11 dígitos numéricos",

# "severity": "ERROR",

# "fieldName": "cpf",

# "createdAt": "2025-11-22T10:20:15"

# }

```

#### Passo 4: Verificar Evento na DLQ

```


# Consultar DLQ

curl http://localhost:8082/api/v1/validation/dlq | jq '.[] | select(.eventId | contains("EMP998"))'

# Resultado esperado:

# {

# "id": 1,

# "eventId": "evt-abc-123",

# "eventType": "CREATE",

# "errorMessage": "INVALID_CPF_FORMAT: CPF inválido",

# "retryCount": 0,

# "maxRetries": 3,

# "status": "PENDING",

# "canRetry": true

# }

```

#### Passo 5: Reprocessar Evento (Após Correção)

```

-- 1. Corrigir CPF na origem
UPDATE source.employees
SET cpf = '12345678901', updated_at = NOW()
WHERE employee_id = 'EMP998';

-- 2. Aguardar CDC (5 segundos)

```

```


# 3. Reprocessar evento da DLQ

curl -X POST http://localhost:8082/api/v1/validation/dlq/1/retry

# Resultado esperado:

# {

# "success": true,

# "message": "Event reprocessed successfully"

# }

```

---

## Módulo 3: Monitoramento e Dashboards (45 min)

### Exercício 4: Navegar Dashboards Grafana

#### Dashboard 1: Overview Geral

**Localização:** Grafana → Dashboards → Overview Geral

**Painéis Importantes:**

1. **Total de Eventos Processados (Hoje)**
   - Número grande no topo
   - Comparação com ontem (%)
   - **Normal:** > 10.000 eventos/dia

2. **Taxa de Sucesso vs Erro**
   - Gráfico de pizza (verde vs vermelho)
   - **Normal:** > 90% verde

3. **Latência P95 (Validação)**
   - Gauge (velocímetro)
   - **Normal:** < 100ms
   - **Alerta:** > 200ms

4. **Throughput (Eventos/minuto)**
   - Gráfico de linha temporal
   - **Normal:** 40-60 evt/min
   - **Pico:** até 100 evt/min

5. **Consumer Lag**
   - Gráfico de área
   - **Normal:** < 100 eventos
   - **Alerta:** > 500 eventos

---

#### Dashboard 2: Validações

**Localização:** Grafana → Dashboards → Validation Dashboard

**Painéis Importantes:**

1. **Erros por Regra de Validação**
   - Gráfico de barras horizontais
   - Top 10 regras mais violadas
   - **Ação:** Se uma regra domina (> 50%), investigar dados origem

2. **Eventos na DLQ (Histórico)**
   - Gráfico de linha
   - **Normal:** Linha estável próxima a zero
   - **Problema:** Linha crescente (acumulando)

3. **Taxa de Reprocessamento DLQ**
   - Percentual de eventos reprocessados com sucesso
   - **Bom:** > 80%
   - **Ruim:** < 50% (dados ruins)

---

### Exercício 5: Criar Alerta Personalizado (Prometheus)

**Objetivo:** Configurar alerta para consumer lag alto.

#### Passo 1: Editar Arquivo de Regras

```


# Editar alert-rules.yml

docker exec -it esocial-prometheus vi /etc/prometheus/alert-rules.yml

```

#### Passo 2: Adicionar Regra

```

groups:

- name: consumer_alerts
rules:
    - alert: ConsumerLagHigh
expr: kafka_consumergroup_lag{group="esocial-consumer-group"} > 1000
for: 5m
labels:
severity: critical
annotations:
summary: "Consumer lag alto detectado"
description: "Lag de {{ \$value }} eventos no grupo esocial-consumer-group"

```

#### Passo 3: Recarregar Prometheus

```


# Reload configuração (sem restart)

curl -X POST http://localhost:9090/-/reload

```

#### Passo 4: Testar Alerta

```


# Simular lag alto (parar consumer temporariamente)

docker stop esocial-consumer-service

# Aguardar 5 minutos

# Verificar alerta disparado

# Prometheus → Alerts → ConsumerLagHigh (Firing)

# Reativar consumer

docker start esocial-consumer-service

```

---

## Módulo 4: Troubleshooting Simulado (60 min)

### Cenário 1: Consumer Lag Alto

**Simulação:**

```


# Instrutor executa (oculto da turma)

# Simular carga alta

docker exec esocial-producer-service bash -c \
"for i in {1..5000}; do echo 'Event \$i'; done"

```

**Sintomas Observáveis:**
- Grafana: Gráfico de Consumer Lag subindo
- Kafka UI: Lag no consumer group aumentando
- Alerta: Email/Slack de "ConsumerLagHigh"

**Sua Missão:** Investigar e resolver.

---

**Investigação (Passo-a-Passo):**

**1. Confirmar o problema**
```


# Ver lag atual

docker exec esocial-kafka-broker-1 kafka-consumer-groups \
--bootstrap-server localhost:9092 \
--describe --group esocial-consumer-group

# Saída:

# TOPIC           PARTITION  LAG

# employee-create    0      2500  ← LAG ALTO!

# employee-create    1      2400

# employee-create    2      2600

```

**2. Verificar saúde do Consumer**
```


# Health check

curl http://localhost:8082/actuator/health | jq

# Logs recentes

docker logs esocial-consumer-service --tail=50 | grep ERROR

```

**3. Verificar recursos (CPU/RAM)**
```


# Stats do container

docker stats esocial-consumer-service --no-stream

# Saída:

# CPU%    MEM USAGE / LIMIT     MEM%

# 85%     1.8GiB / 2GiB         90%   ← Memória alta!

```

**4. Identificar causa raiz**
- Consumer lento (processamento complexo)
- Pico de carga repentino
- Memória insuficiente (GC excessivo)

**5. Soluções possíveis**

**Solução A: Aumentar Heap JVM (temporário)**
```


# Editar docker-compose.yml

environment:

- JAVA_OPTS=-Xmx2g -Xms1g  \# Aumentar de 1GB para 2GB


# Restart

docker-compose restart consumer-service

```

**Solução B: Escalar Consumer (produção)**
```


# Kubernetes (se disponível)

kubectl scale deployment consumer-service --replicas=3

# Resultado: 3 consumers processando em paralelo

```

**Solução C: Aumentar Partições (longo prazo)**
```


# Mais partições = mais paralelismo

docker exec esocial-kafka-broker-1 kafka-topics \
--bootstrap-server localhost:9092 \
--alter --topic employee-create --partitions 6

```

**6. Validar resolução**
```


# Monitorar lag diminuindo

watch -n 5 'docker exec esocial-kafka-broker-1 kafka-consumer-groups \
--bootstrap-server localhost:9092 \
--describe --group esocial-consumer-group | grep LAG'

# Lag deve voltar para < 100 em ~10 minutos

```

✅ **Problema Resolvido!**

---

### Cenário 2: Taxa de Erro Elevada (> 15%)

**Simulação:**

```


# Instrutor insere 50 registros com CPF inválido

docker exec esocial-postgres-db psql -U esocial_user -d esocial -c \
"INSERT INTO source.employees (employee_id, cpf, ...)
SELECT 'EMP' || generate_series(1, 50), '123', ...;"

```

**Sintomas Observáveis:**
- Grafana: Taxa de Erro subiu para 25%
- API: 50+ erros em `/api/v1/validation/errors`
- DLQ: Acumulando eventos

**Sua Missão:** Identificar causa e mitigar.

---

**Investigação:**

**1. Consultar erros recentes**
```

curl http://localhost:8082/api/v1/validation/errors | \
jq 'group_by(.ruleName) | map({rule: ..ruleName, count: length}) | sort_by(.count) | reverse'

# Saída:

# { "rule": "INVALID_CPF_FORMAT", "count": 50 },  ← Problema identificado!

# { "rule": "MINIMUM_AGE", "count": 3 }

# ]

```

**2. Analisar padrão dos erros**
```


# Ver exemplos de erros

curl http://localhost:8082/api/v1/validation/errors | \
jq '.[] | select(.ruleName == "INVALID_CPF_FORMAT") | {sourceId, cpf: .fieldValue}' | head -5

# Saída:

# { "sourceId": "EMP1", "cpf": "123" }

# { "sourceId": "EMP2", "cpf": "123" }

# ...

# Padrão: Todos têm CPF "123" (claramente inválido)

```

**3. Identificar origem**
- Problema no sistema RH (entrada de dados)
- Migração de dados mal feita
- Bug no sistema legado

**4. Ações corretivas**

**Ação A: Corrigir dados na origem**
```

-- Conectar no PostgreSQL
docker exec -it esocial-postgres-db psql -U esocial_user -d esocial

-- Corrigir CPFs inválidos (exemplo: padronizar com zeros)
UPDATE source.employees
SET cpf = lpad(cpf, 11, '0'), updated_at = NOW()
WHERE length(cpf) < 11;

-- Resultado: 50 rows updated

```

**Ação B: Reprocessar DLQ**
```


# Listar IDs na DLQ

curl http://localhost:8082/api/v1/validation/dlq | jq '.[].id'

# Reprocessar um por vez (ou script)

for id in {1..50}; do
curl -X POST http://localhost:8082/api/v1/validation/dlq/\$id/retry
echo "Reprocessado ID \$id"
sleep 1
done

```

**Ação C: Comunicar área de RH**
```

Assunto: [URGENTE] Erro de validação em massa - CPF

Time de RH,

Detectamos 50 registros com CPF inválido ("123") inseridos hoje.

Causa: [Identificar com RH]
Correção: Já aplicada automaticamente
Impacto: Eventos reprocessados com sucesso

Por favor, revisar processo de entrada de dados para evitar recorrência.

Atenciosamente,
Time de Operações

```

**5. Validar resolução**
```


# Dashboard deve voltar ao normal

curl http://localhost:8082/api/v1/validation/dashboard | jq '.successRate'

# Resultado esperado: > 90%

```

✅ **Problema Resolvido!**

---

### Cenário 3: Kafka Broker Down

**Simulação:**

```


# Instrutor derruba 1 broker (de 3)

docker stop esocial-kafka-broker-2

```

**Sintomas Observáveis:**
- Kafka UI: Apenas 2 brokers ativos
- Logs Producer/Consumer: Warnings de conexão
- Prometheus: Alerta "KafkaBrokerDown"

**Sua Missão:** Diagnosticar e restaurar.

---

**Investigação:**

**1. Confirmar broker down**
```


# Listar brokers

docker exec esocial-kafka-broker-1 kafka-broker-api-versions \
--bootstrap-server localhost:9092

# Saída:

# esocial-kafka-broker-1:9092 (id: 1) ← OK

# esocial-kafka-broker-3:9094 (id: 3) ← OK

# Falta broker-2!

```

**2. Verificar status do container**
```

docker ps -a | grep kafka-broker-2

# Saída:

# Exited (137) 5 minutes ago  ← Container parado!

```

**3. Verificar logs do broker**
```

docker logs esocial-kafka-broker-2 --tail=50

# Buscar por:

# - OutOfMemoryError

# - CorruptRecordException

# - Disk full

```

**4. Restart do broker**
```


# Tentar restart simples

docker start esocial-kafka-broker-2

# Aguardar inicialização (~30 segundos)

sleep 30

# Verificar logs

docker logs -f esocial-kafka-broker-2 | grep "started"

# Aguardar: "Kafka Server started"

```

**5. Validar recuperação**
```


# Verificar 3 brokers ativos

docker exec esocial-kafka-broker-1 kafka-broker-api-versions \
--bootstrap-server localhost:9092 | wc -l

# Resultado esperado: 3

# Verificar replicação OK

docker exec esocial-kafka-broker-1 kafka-topics \
--bootstrap-server localhost:9092 \
--describe --topic employee-create

# Verificar: ISR (In-Sync Replicas) = 3

```

**6. Se restart falhar**
```


# Problema grave (disco cheio, corrupção)

# ESCALAR PARA DBA/INFRA!

# Temporário: Sistema continua funcionando com 2 brokers

# (replication factor = 3, min.insync.replicas = 2)

```

✅ **Broker restaurado! Sistema estável.**

---

## Módulo 5: Q&A e Feedback (30 min)

### Perguntas Frequentes (FAQ)

**Q1: O que faço se TODOS os brokers Kafka caírem?**

**R:** Pânico controlado! 😅
1. Sistema para de funcionar (Producer não publica, Consumer não consome)
2. Verificar causa (Zookeeper down? Rede? Disco?)
3. ESCALAR IMEDIATAMENTE para Infra/DBA
4. Enquanto isso: Dados ficam acumulados no PostgreSQL origem (CDC reprocesa)
5. Após recovery: Sistema volta sozinho (eventos não foram perdidos)

---

**Q2: Posso apagar eventos da DLQ?**

**R:** ⚠️ Com cuidado!
- **SIM** se evento foi reprocessado com sucesso (status = REPROCESSED)
- **SIM** se evento é lixo confirmado (ex: teste)
- **NÃO** se ainda pode ser corrigido (status = PENDING/FAILED)
- **NUNCA** sem approval do negócio (RH)

```

-- Apagar apenas reprocessados
DELETE FROM dlq_events WHERE status = 'REPROCESSED' AND updated_at < NOW() - INTERVAL '7 days';

```

---

**Q3: Quanto tempo posso deixar o Consumer parado?**

**R:**
- **Kafka:** Retém mensagens por 7 dias (configurado)
- **Risco:** Consumer lag cresce (~5.000 evt/hora)
- **Máximo seguro:** 4 horas (lag recuperável em 1 hora)
- **Crítico:** > 12 horas (lag muito alto, pode demorar dias para recuperar)

---

**Q4: Como sei se preciso escalar o Consumer?**

**R:** Indicadores:
- ✅ **Escalar** se:
  - Consumer lag > 1.000 persistente (> 1 hora)
  - CPU > 80% contínuo
  - Latência P95 > 200ms
  
- ❌ **NÃO escalar** se:
  - Lag temporário (pico de carga)
  - Recursos OK (CPU < 50%)

---

### Formulário de Feedback

```


# Treinamento Pipeline ETL eSocial - Feedback

**Data:** 2025-11-22
**Seu Nome:** _______________________
**Cargo:** _______________________

## Avaliação do Conteúdo

| Módulo | Clareza (1-5) | Utilidade (1-5) | Comentários |
| :-- | :-- | :-- | :-- |
| Arquitetura | ⬜1 ⬜2 ⬜3 ⬜4 ⬜5 | ⬜1 ⬜2 ⬜3 ⬜4 ⬜5 |  |
| Hands-On | ⬜1 ⬜2 ⬜3 ⬜4 ⬜5 | ⬜1 ⬜2 ⬜3 ⬜4 ⬜5 |  |
| Monitoramento | ⬜1 ⬜2 ⬜3 ⬜4 ⬜5 | ⬜1 ⬜2 ⬜3 ⬜4 ⬜5 |  |
| Troubleshooting | ⬜1 ⬜2 ⬜3 ⬜4 ⬜5 | ⬜1 ⬜2 ⬜3 ⬜4 ⬜5 |  |

## Questões

**1. Você se sente preparado para operar o sistema?**

- ⬜ Sim, totalmente
- ⬜ Sim, mas preciso de mais prática
- ⬜ Parcialmente
- ⬜ Não

**2. Qual módulo foi mais útil?**

- ⬜ Arquitetura
- ⬜ Hands-On
- ⬜ Monitoramento
- ⬜ Troubleshooting

**3. O que faltou no treinamento?**

________________________________________________

**4. Sugestões de melhoria:**

________________________________________________

**5. Dúvidas que ficaram:**

________________________________________________

**Obrigado pelo feedback!**

```

---

## Anexo A: Comandos Essenciais (Cheat Sheet)

```


# ====================================

# HEALTH CHECKS

# ====================================

# Producer

curl http://localhost:8081/actuator/health | jq

# Consumer

curl http://localhost:8082/actuator/health | jq

# Prometheus

curl http://localhost:9090/-/healthy

# Grafana

curl http://localhost:3000/api/health

# ====================================

# MONITORAMENTO

# ====================================

# Ver métricas Producer

curl http://localhost:8081/actuator/prometheus | grep events_published

# Ver métricas Consumer

curl http://localhost:8082/actuator/prometheus | grep events_consumed

# Ver consumer lag

docker exec esocial-kafka-broker-1 kafka-consumer-groups \
--bootstrap-server localhost:9092 \
--describe --group esocial-consumer-group

# ====================================

# VALIDAÇÕES E DLQ

# ====================================

# Listar erros

curl http://localhost:8082/api/v1/validation/errors | jq

# Dashboard de validação

curl http://localhost:8082/api/v1/validation/dashboard | jq

# Listar eventos DLQ

curl http://localhost:8082/api/v1/validation/dlq | jq

# Reprocessar evento DLQ

curl -X POST http://localhost:8082/api/v1/validation/dlq/{id}/retry

# ====================================

# CONTAINERS

# ====================================

# Listar todos

docker-compose ps

# Restart serviço

docker-compose restart producer-service

# Ver logs

docker-compose logs -f consumer-service

# Stats (CPU/RAM)

docker stats esocial-consumer-service --no-stream

# ====================================

# KAFKA

# ====================================

# Listar tópicos

docker exec esocial-kafka-broker-1 kafka-topics \
--bootstrap-server localhost:9092 --list

# Descrever tópico

docker exec esocial-kafka-broker-1 kafka-topics \
--bootstrap-server localhost:9092 \
--describe --topic employee-create

# Ver mensagens (últimas 10)

docker exec esocial-kafka-broker-1 kafka-console-consumer \
--bootstrap-server localhost:9092 \
--topic employee-create \
--from-beginning --max-messages 10

# ====================================

# POSTGRESQL

# ====================================

# Conectar

docker exec -it esocial-postgres-db psql -U esocial_user -d esocial

# Queries úteis

SELECT COUNT(*) FROM public.employees;
SELECT * FROM audit.employees_history ORDER BY changed_at DESC LIMIT 10;
SELECT COUNT(*) FROM dlq_events WHERE status = 'PENDING';

```

---

**Última atualização:** 2025-11-22  
**Versão:** 1.0  
**Autor:** Márcio Kuroki Gonçalves