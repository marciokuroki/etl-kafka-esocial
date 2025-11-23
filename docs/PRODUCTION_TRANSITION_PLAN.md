# Plano de Transição para Produção - Pipeline ETL eSocial

**Versão:** 1.0  
**Data:** 2025-11-22  
**Projeto:** Pipeline ETL eSocial  
**Responsável:** Márcio Kuroki Gonçalves

**⚠️ IMPORTANTE:** Este é um plano **teórico/simulado** desenvolvido para o Projeto Aplicado. Em ambiente corporativo real, este plano seria validado com as áreas de Infraestrutura, Segurança, Compliance e Business.

---

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Estratégia de Cutover](#estratégia-de-cutover)
3. [Pré-requisitos e Checklist Pré-Produção](#pré-requisitos-e-checklist-pré-produção)
4. [Plano de Rollback](#plano-de-rollback)
5. [Janela de Manutenção](#janela-de-manutenção)
6. [Processo de Homologação](#processo-de-homologação)
7. [Checklist Pós-Produção](#checklist-pós-produção)
8. [Comunicação e Stakeholders](#comunicação-e-stakeholders)
9. [Contingências](#contingências)

---

## Visão Geral

### Objetivo da Transição

Substituir o **sistema legado de integração eSocial** (baseado em batch jobs + FTP) pelo **novo Pipeline ETL event-driven** utilizando Kafka, garantindo:

- ✅ **Zero perda de dados** durante a transição
- ✅ **Downtime mínimo** (janela de 4 horas)
- ✅ **Rollback seguro** em caso de problemas críticos
- ✅ **Validação completa** antes de desativar sistema legado

### Sistemas Envolvidos

| Sistema | Papel | Status Atual | Status Futuro |
|---------|-------|--------------|---------------|
| **Sistema RH Legado** | Origem dos dados | ✅ Ativo | ✅ Permanece (PostgreSQL) |
| **Batch ETL Legado** | Processamento atual | ✅ Ativo | ❌ Desativado (após transição) |
| **FTP eSocial** | Envio arquivos XML | ✅ Ativo | ❌ Substituído por API |
| **Pipeline Kafka (Novo)** | Streaming event-driven | 🟡 Homologação | ✅ Produção |
| **Portal eSocial** | Destino final | ✅ Ativo | ✅ Permanece |

### Riscos Identificados

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| **Perda de dados na transição** | Baixa | CRÍTICO | Execução paralela (7 dias) + validação |
| **Incompatibilidade com eSocial** | Média | ALTO | Homologação em ambiente gov.br |
| **Performance insuficiente** | Baixa | ALTO | Testes de carga (10k evt/s) |
| **Rollback demorado** | Média | ALTO | Plano de rollback testado |
| **Falha humana** | Média | MÉDIO | Checklists + dupla validação |

---

## Estratégia de Cutover

### Opções Avaliadas

#### Opção 1: Big Bang (Descartada)

**Descrição:** Desligar sistema legado e ligar novo sistema de uma vez.

**Vantagens:**
- ✅ Transição rápida (1 dia)
- ✅ Menos complexo

**Desvantagens:**
- ❌ Alto risco de falha
- ❌ Rollback difícil
- ❌ Zero margem de erro

**Decisão:** ❌ **Descartada** (risco muito alto)

---

#### Opção 2: Phased Rollout por Região (Considerada)

**Descrição:** Migrar por filiais/regiões progressivamente.

**Vantagens:**
- ✅ Risco distribuído
- ✅ Aprendizado incremental

**Desvantagens:**
- ❌ Complexidade operacional (2 sistemas paralelos por meses)
- ❌ Custo elevado de manutenção dual

**Decisão:** 🟡 **Reserva** (se Opção 3 falhar)

---

#### Opção 3: Parallel Run com Cutover Planejado (ESCOLHIDA) ✅

**Descrição:** Executar ambos os sistemas em paralelo por 7 dias, validar resultados e fazer cutover planejado.

**Vantagens:**
- ✅ Validação real de dados (comparação lado-a-lado)
- ✅ Rollback trivial (apenas desligar novo sistema)
- ✅ Confiança alta antes do cutover
- ✅ Zero perda de dados

**Desvantagens:**
- ⚠️ Requer 7 dias de execução dual
- ⚠️ Custo computacional temporário (2x)

**Decisão:** ✅ **ESCOLHIDA**

---

### Fases da Transição (Parallel Run)

```

┌──────────────┬──────────────┬──────────────┬──────────────┐
│   Fase 1     │   Fase 2     │   Fase 3     │   Fase 4     │
│  Preparação  │ Parallel Run │   Cutover    │ Pós-Produção │
│   (3 dias)   │   (7 dias)   │  (4 horas)   │   (7 dias)   │
└──────────────┴──────────────┴──────────────┴──────────────┘
D-10 a D-7      D-7 a D-0        D-Day       D+1 a D+7

Fase 1: Preparação

- Deploy em produção (modo shadowing)
- Configurações finais
- Treinamento equipe

Fase 2: Parallel Run (Validação)

- Legado: 100% tráfego (ativo)
- Novo: 100% tráfego (shadowing - não envia para eSocial)
- Comparar resultados diariamente

Fase 3: Cutover

- Janela de manutenção (Sábado 02:00 - 06:00)
- Desligar batch legado
- Ativar envio real do novo sistema
- Validação intensiva

Fase 4: Pós-Produção

- Monitoramento 24x7
- Suporte on-call
- Manter legado desligado (standby para rollback)

```

---

## Pré-requisitos e Checklist Pré-Produção

### Pré-requisitos Obrigatórios

#### Infraestrutura

- [ ] **Cluster Kafka Production-ready**
  - 3 brokers (mínimo)
  - Replication Factor = 3
  - Min In-Sync Replicas = 2
  - Disk: 1TB por broker (SSD)
  - CPU: 8 cores por broker
  - RAM: 32GB por broker

- [ ] **PostgreSQL Production-ready**
  - Instância dedicada (não compartilhada)
  - CPU: 16 cores
  - RAM: 64GB
  - Disk: 500GB SSD (NVMe)
  - Replicação ativa (primary + standby)
  - Backup automatizado (diário + incremental)

- [ ] **Kubernetes Cluster** (ou Docker Swarm)
  - 5 nodes (mínimo)
  - Auto-scaling configurado
  - Health checks ativos
  - Load balancer configurado

- [ ] **Rede e Segurança**
  - VPN para acesso portal eSocial
  - Certificado Digital A1 ou A3 instalado
  - TLS 1.3 habilitado (Kafka + PostgreSQL)
  - SASL/SCRAM autenticação Kafka
  - Firewall rules configuradas

#### Observabilidade

- [ ] **Stack de Monitoramento**
  - Prometheus (coleta de métricas)
  - Grafana (dashboards)
  - Alertmanager (alertas)
  - Jaeger ou Zipkin (distributed tracing)
  - ELK Stack (logs centralizados)

- [ ] **Alertas Configurados**
  - 15 alertas críticos ativos
  - PagerDuty ou similar integrado
  - Escalação automática configurada
  - Runbooks documentados

#### Segurança e Compliance

- [ ] **LGPD / Proteção de Dados**
  - Dados sensíveis criptografados (at-rest + in-transit)
  - Logs de acesso habilitados
  - Retenção de dados configurada (7 anos eSocial)
  - Anonimização de dados de teste

- [ ] **Auditoria**
  - Audit trail completo
  - Registros de mudanças (quem, quando, o quê)
  - Logs imutáveis (WORM - Write Once, Read Many)

#### Testes

- [ ] **Testes de Aceitação (UAT)**
  - 50 cenários de teste executados
  - 100% de sucesso nos cenários críticos
  - Sign-off das áreas de negócio

- [ ] **Testes de Carga**
  - Throughput: 10.000 eventos/segundo (pico)
  - Latência P95 < 100ms
  - Stress test: 24 horas contínuas
  - Resultado: ✅ Sistema estável

- [ ] **Testes de Integração eSocial**
  - Ambiente de homologação gov.br
  - 100 eventos de teste enviados
  - Validação de retorno (protocolos)
  - Certificado digital validado

#### Documentação

- [ ] **Documentação Completa**
  - Manual de Operação (OPERATIONS_MANUAL.md)
  - Manual do Desenvolvedor (DEVELOPER_GUIDE.md)
  - Runbooks de incidentes
  - ADRs (7 decisões arquiteturais)
  - Diagramas C4 Model atualizados

- [ ] **Treinamento**
  - Equipe de operações treinada (8 horas)
  - Equipe de suporte treinada (4 horas)
  - Simulação de incidentes realizada

---

### Checklist Pré-Produção (D-7)

#### Semana Antes do Go-Live

| # | Atividade | Responsável | Prazo | Status |
|---|-----------|-------------|-------|--------|
| 1 | Deploy em produção (modo shadowing) | DevOps | D-7 | ⬜ |
| 2 | Configurar variáveis de ambiente | DevOps | D-7 | ⬜ |
| 3 | Validar conectividade com eSocial | Infra | D-7 | ⬜ |
| 4 | Configurar certificado digital | Segurança | D-7 | ⬜ |
| 5 | Habilitar TLS/SASL no Kafka | Segurança | D-6 | ⬜ |
| 6 | Ativar monitoramento e alertas | DevOps | D-6 | ⬜ |
| 7 | Backup completo do PostgreSQL | DBA | D-6 | ⬜ |
| 8 | Executar smoke tests | QA | D-5 | ⬜ |
| 9 | Iniciar Parallel Run | DevOps | D-7 | ⬜ |
| 10 | Validar resultados Parallel Run (diário) | Tech Lead | D-7 a D-1 | ⬜ |
| 11 | Reunião Go/No-Go | Todos | D-1 | ⬜ |
| 12 | Comunicar stakeholders | PM | D-1 | ⬜ |

---

#### Checklist 1 Dia Antes (D-1)

| # | Atividade | Responsável | Status |
|---|-----------|-------------|--------|
| 1 | Validar resultados finais Parallel Run | Tech Lead | ⬜ |
| 2 | Comparar 100% dos eventos (legado vs novo) | QA | ⬜ |
| 3 | Confirmar taxa de sucesso > 99.9% | QA | ⬜ |
| 4 | Executar health checks completos | DevOps | ⬜ |
| 5 | Verificar espaço em disco (> 50% livre) | Infra | ⬜ |
| 6 | Confirmar equipe on-call disponível | PM | ⬜ |
| 7 | Testar plano de rollback (simulação) | DevOps | ⬜ |
| 8 | Freeze de mudanças em produção | Change Manager | ⬜ |
| 9 | Comunicar usuários finais (RH) | PM | ⬜ |
| 10 | Reunião Final Go/No-Go | Comitê | ⬜ |

**Critério Go:** Todos os itens devem estar ✅ para prosseguir.

---

## Plano de Rollback

### Cenários de Rollback

| Cenário | Gatilho | Tempo de Decisão | Ação |
|---------|---------|------------------|------|
| **Rollback Imediato** | Taxa de erro > 20% | 15 minutos | Desligar novo sistema, reativar legado |
| **Rollback Planejado** | Performance insatisfatória | 2 horas | Análise + decisão + rollback |
| **Rollback Parcial** | Problema específico | 1 hora | Desabilitar funcionalidade afetada |

---

### Procedimento de Rollback Completo

#### Passo-a-Passo (30 minutos)

**1. Declarar Rollback (5 min)**

```


# Líder técnico declara oficialmente

echo "ROLLBACK INICIADO - \$(date)" | tee /var/log/rollback.log

# Notificar equipe

curl -X POST https://slack.com/api/chat.postMessage \
-d "channel=\#alerts-esocial" \
-d "text=🚨 ROLLBACK EM ANDAMENTO"

```

**2. Desligar Novo Sistema (5 min)**

```


# Parar Kafka Consumer (para de consumir eventos)

kubectl scale deployment consumer-service --replicas=0

# Aguardar 30 segundos

sleep 30

# Parar Kafka Producer (para de publicar eventos)

kubectl scale deployment producer-service --replicas=0

# Verificar que não há pods rodando

kubectl get pods | grep esocial

```

**3. Reativar Sistema Legado (10 min)**

```


# Conectar no servidor legado

ssh admin@legacy-etl-server

# Reativar cron jobs

crontab -e

# Descomentar:

# 0 2 * * * /opt/etl/batch-esocial.sh

# Iniciar manualmente (não esperar cron)

sudo systemctl start etl-esocial.service

# Verificar logs

tail -f /var/log/etl-esocial.log

# Aguardar: "ETL process started successfully"

```

**4. Validar Sistema Legado (5 min)**

```


# Executar smoke test

/opt/etl/tests/smoke-test.sh

# Verificar última execução

psql -U admin -d esocial -c \
"SELECT MAX(processed_at) FROM etl_execution_log;"

# Deve ser < 5 minutos atrás

# Verificar arquivos XML gerados

ls -lh /opt/etl/output/xml/ | tail -10

```

**5. Comunicar Rollback (5 min)**

```


# Notificar stakeholders

cat > /tmp/rollback-notification.txt << EOF
Assunto: [URGENTE] Rollback do Pipeline ETL eSocial

Equipe,

Foi necessário realizar rollback do novo Pipeline ETL eSocial para o sistema legado.

Motivo: [DESCREVER MOTIVO]
Horário: \$(date)
Sistemas afetados: Pipeline Kafka (desligado), Batch ETL (reativado)
Impacto: Processamento voltou ao normal via sistema legado
Próximos passos: Análise de causa raiz e novo plano de deploy

Status: Sistema legado OPERACIONAL
Downtime total: XX minutos

Atenciosamente,
Time DevOps
EOF

# Enviar para lista de distribuição

mail -s "[URGENTE] Rollback ETL eSocial" stakeholders@empresa.com \
< /tmp/rollback-notification.txt

```

---

### Validação Pós-Rollback

| # | Validação | Comando | Resultado Esperado |
|---|-----------|---------|-------------------|
| 1 | Sistema legado rodando | `systemctl status etl-esocial` | active (running) |
| 2 | Últimos eventos processados | `SELECT COUNT(*) FROM esocial_events WHERE processed_at > NOW() - INTERVAL '1 hour'` | > 0 |
| 3 | Arquivos XML gerados | `ls /opt/etl/output/xml/ \| wc -l` | > 10 |
| 4 | Novo sistema desligado | `kubectl get pods \| grep esocial` | No resources found |
| 5 | Usuários RH operando | Contato manual | ✅ OK |

---

### Post-Mortem Obrigatório

Após rollback, agendar reunião de post-mortem em **24 horas**:

**Agenda:**
1. Linha do tempo do incidente
2. Causa raiz (5 Whys)
3. Impacto (usuários, dados, financeiro)
4. O que funcionou / o que falhou
5. Ações corretivas (com responsáveis e prazos)
6. Novo plano de deploy (se aplicável)

---

## Janela de Manutenção

### Janela Planejada

**Data:** Sábado, [DATA], 02:00 - 06:00 (4 horas)  
**Fuso Horário:** America/Sao_Paulo (GMT-3)  
**Justificativa:** Menor volume de transações (< 1% do volume diário)

### Análise de Impacto

| Horário | Volume Eventos | % Volume Diário | Impacto Usuários |
|---------|----------------|-----------------|------------------|
| 00:00 - 02:00 | 500 | 2% | Baixo (plantão) |
| **02:00 - 06:00** | **200** | **< 1%** | **Mínimo** |
| 06:00 - 08:00 | 1.200 | 5% | Médio |
| 08:00 - 18:00 | 18.000 | 75% | Alto |

**Decisão:** Janela de 02:00 - 06:00 minimiza impacto.

---

### Cronograma Detalhado da Janela

| Horário | Duração | Atividade | Responsável | Rollback Point |
|---------|---------|-----------|-------------|----------------|
| 01:45 | 15 min | Reunião War Room (kick-off) | Todos | - |
| 02:00 | 10 min | Freeze do sistema legado | DevOps | ✅ RP1 |
| 02:10 | 10 min | Backup final PostgreSQL | DBA | ✅ RP2 |
| 02:20 | 20 min | Desligar batch ETL legado | DevOps | ✅ RP3 |
| 02:40 | 10 min | Ativar Producer (novo sistema) | DevOps | - |
| 02:50 | 10 min | Ativar Consumer (novo sistema) | DevOps | - |
| 03:00 | 30 min | Smoke tests + validação | QA | ✅ RP4 |
| 03:30 | 30 min | Processar backlog (eventos da janela) | DevOps | - |
| 04:00 | 60 min | Monitoramento intensivo | Todos | ✅ RP5 |
| 05:00 | 30 min | Validação final + dashboards | Tech Lead | - |
| 05:30 | 30 min | Reunião Go-Live / Lessons Learned | Todos | - |
| 06:00 | - | Fim da janela de manutenção | - | - |

**Rollback Points (RP):** Momentos onde rollback pode ser iniciado com segurança.

---

### Comunicação Durante a Janela

**Slack Channel:** #deploy-esocial-cutover

**Frequência de Updates:**
- A cada 15 minutos (status)
- Imediato (se problema crítico)

**Template de Update:**
```

[HH:MM] Status Update

✅ Concluído: [Atividade]
🔄 Em andamento: [Atividade]
⏳ Próximo: [Atividade]

Problemas: Nenhum / [Descrever]
Decisão: Prosseguir / Rollback

Próximo update: HH:MM

```

---

## Processo de Homologação

### Ambientes de Validação

| Ambiente | Propósito | Dados | Duração |
|----------|-----------|-------|---------|
| **Dev** | Desenvolvimento | Sintéticos | Contínuo |
| **QA** | Testes automatizados | Sintéticos | Contínuo |
| **Staging** | Testes manuais | Anonimizados (produção) | 2 semanas |
| **Pre-Prod** | Parallel Run | Produção (shadowing) | 7 dias |
| **Produção** | Go-Live | Produção (real) | - |

---

### Checklist de Homologação (Staging)

#### Funcionalidades

- [ ] **CDC (Change Data Capture)**
  - [ ] Detectar INSERT em < 5 segundos
  - [ ] Detectar UPDATE em < 5 segundos
  - [ ] Detectar DELETE em < 5 segundos
  - [ ] Não publicar eventos duplicados

- [ ] **Validações**
  - [ ] Camada 1 (Estrutural): 6 regras funcionando
  - [ ] Camada 2 (Negócio): 5 regras funcionando
  - [ ] Fail-fast (para no primeiro ERROR)
  - [ ] Warnings não bloqueiam processamento

- [ ] **Persistência**
  - [ ] Dados persistidos corretamente
  - [ ] Versionamento otimista funcionando
  - [ ] Audit trail completo (trigger)
  - [ ] Offset Kafka único (+ partition)

- [ ] **DLQ (Dead Letter Queue)**
  - [ ] Eventos inválidos vão para DLQ
  - [ ] Reprocessamento manual funcionando
  - [ ] Max retries respeitado (3 tentativas)

- [ ] **APIs REST**
  - [ ] GET /api/v1/validation/errors (200 OK)
  - [ ] GET /api/v1/validation/dashboard (200 OK)
  - [ ] GET /api/v1/validation/dlq (200 OK)
  - [ ] POST /api/v1/validation/dlq/{id}/retry (200 OK)

#### Performance

- [ ] **Throughput**
  - [ ] Processar 1.000 eventos/segundo (mínimo)
  - [ ] Processar 10.000 eventos/segundo (pico)
  - [ ] Zero perda de dados em 24 horas

- [ ] **Latência**
  - [ ] P50 < 50ms (validação)
  - [ ] P95 < 100ms (validação)
  - [ ] P99 < 200ms (validação)

- [ ] **Recursos**
  - [ ] CPU Consumer < 70% (normal)
  - [ ] RAM Consumer < 2GB
  - [ ] Heap JVM estável (sem memory leak)

#### Observabilidade

- [ ] **Métricas**
  - [ ] Prometheus coletando 15 métricas
  - [ ] Dashboards Grafana carregando
  - [ ] Alertas disparando corretamente (teste)

- [ ] **Logs**
  - [ ] Logs estruturados (JSON)
  - [ ] Correlation ID em todos os logs
  - [ ] Logs centralizados (ELK ou similar)

#### Segurança

- [ ] **Autenticação e Autorização**
  - [ ] TLS 1.3 habilitado (Kafka + PostgreSQL)
  - [ ] SASL/SCRAM autenticação Kafka
  - [ ] Certificado Digital A1 válido

- [ ] **Criptografia**
  - [ ] Dados em trânsito criptografados
  - [ ] Dados em repouso criptografados
  - [ ] Senhas não aparecem em logs

---

### Sign-off de Homologação

| Área | Responsável | Data | Assinatura | Observações |
|------|-------------|------|------------|-------------|
| **Desenvolvimento** | Márcio Kuroki | [DATA] | ⬜ | - |
| **QA/Testes** | [Nome] | [DATA] | ⬜ | - |
| **Segurança** | [Nome] | [DATA] | ⬜ | - |
| **Infraestrutura** | [Nome] | [DATA] | ⬜ | - |
| **DBA** | [Nome] | [DATA] | ⬜ | - |
| **RH (Negócio)** | [Nome] | [DATA] | ⬜ | - |
| **Compliance** | [Nome] | [DATA] | ⬜ | - |

**Critério de Aprovação:** ✅ **TODAS as áreas devem aprovar** para prosseguir para Parallel Run.

---

## Checklist Pós-Produção

### Primeiras 24 Horas (D+1)

| # | Atividade | Frequência | Responsável | Status |
|---|-----------|------------|-------------|--------|
| 1 | Monitorar dashboards Grafana | Contínuo | DevOps | ⬜ |
| 2 | Verificar alertas disparados | A cada hora | DevOps | ⬜ |
| 3 | Validar eventos processados | A cada 4h | QA | ⬜ |
| 4 | Comparar com sistema legado | 2x/dia | Tech Lead | ⬜ |
| 5 | Verificar DLQ (não deve acumular) | A cada 2h | DevOps | ⬜ |
| 6 | Revisar logs de erro | A cada 4h | DevOps | ⬜ |
| 7 | Validar integração eSocial | 1x/dia | QA | ⬜ |
| 8 | Coletar feedback usuários RH | 1x/dia | PM | ⬜ |
| 9 | Reunião de status | 3x/dia | Todos | ⬜ |

---

### Primeira Semana (D+1 a D+7)

| # | Atividade | Prazo | Responsável | Status |
|---|-----------|-------|-------------|--------|
| 1 | Análise de métricas (throughput, latência) | D+1 | Tech Lead | ⬜ |
| 2 | Relatório de erros (se houver) | D+2 | QA | ⬜ |
| 3 | Validação de 100% dos eventos (amostragem) | D+3 | QA | ⬜ |
| 4 | Ajustes finos (se necessário) | D+1 a D+5 | DevOps | ⬜ |
| 5 | Treinamento adicional (se gaps identificados) | D+4 | PM | ⬜ |
| 6 | Documentação de lições aprendidas | D+5 | Tech Lead | ⬜ |
| 7 | Descomissionar sistema legado (standby → off) | D+7 | Infra | ⬜ |
| 8 | Celebração com equipe 🎉 | D+7 | PM | ⬜ |

---

### Métricas de Sucesso (Pós-Produção)

| Métrica | Baseline (Legado) | Target (Novo) | Real (D+7) | Status |
|---------|-------------------|---------------|------------|--------|
| **Throughput** | 800 evt/s | 1.200 evt/s | - | ⬜ |
| **Latência P95** | 2.500ms (batch) | < 100ms | - | ⬜ |
| **Taxa de Sucesso** | 95% | > 99% | - | ⬜ |
| **Uptime** | 98% | > 99.7% | - | ⬜ |
| **Incidentes Críticos** | 5/mês | < 1/mês | - | ⬜ |
| **Satisfação Usuários** | 6/10 | > 8/10 | - | ⬜ |

**Critério de Sucesso:** 100% das métricas atingindo ou superando o target.

---

## Comunicação e Stakeholders

### Matriz RACI

| Atividade | Responsável (R) | Aprovador (A) | Consultado (C) | Informado (I) |
|-----------|-----------------|---------------|----------------|---------------|
| **Plano de Transição** | Tech Lead | CTO | DevOps, DBA | Todos |
| **Parallel Run** | DevOps | Tech Lead | QA | PM, RH |
| **Go/No-Go Decision** | Comitê | CTO | Tech Lead | Todos |
| **Execução Cutover** | DevOps | Tech Lead | DBA, Infra | PM, RH |
| **Rollback** | Tech Lead | CTO | DevOps | Todos |
| **Sign-off Final** | Tech Lead | CTO, RH | QA | Todos |

---

### Plano de Comunicação

#### Antes do Go-Live (D-7 a D-1)

| Público | Mensagem | Canal | Frequência |
|---------|----------|-------|------------|
| **Time Técnico** | Status diário Parallel Run | Slack #esocial-deploy | Diário |
| **Gestão RH** | Preparação para mudança | Email + reunião | D-7, D-3, D-1 |
| **Usuários RH** | Novidades do sistema | Intranet + treinamento | D-3 |
| **C-Level** | Status executivo | Email | D-7, D-3, D-1 |

#### Durante Go-Live (D-Day)

| Público | Mensagem | Canal | Frequência |
|---------|----------|-------|------------|
| **Time Técnico** | Status em tempo real | Slack + War Room | A cada 15 min |
| **Gestão RH** | Updates críticos | Email + WhatsApp | Se problema |
| **C-Level** | Status executivo | Email | Início, meio, fim |

#### Pós Go-Live (D+1 a D+7)

| Público | Mensagem | Canal | Frequência |
|---------|----------|-------|------------|
| **Time Técnico** | Status diário | Slack | Diário |
| **Gestão RH** | Relatório de sucesso | Email + reunião | D+1, D+3, D+7 |
| **Usuários RH** | Feedback e dúvidas | Helpdesk + FAQ | Contínuo |
| **C-Level** | Relatório executivo final | Apresentação | D+7 |

---

### Template de Comunicação (Go-Live)

#### Email para Usuários RH (D-1)

```

Assunto: [IMPORTANTE] Nova Solução ETL eSocial - Go-Live Sábado

Prezados(as),

Informamos que no próximo sábado, [DATA], das 02:00 às 06:00,
realizaremos a transição para o novo Pipeline ETL eSocial.

O QUE MUDA:
✅ Processamento em tempo real (vs batch diário)
✅ Validações automáticas aprimoradas
✅ Interface de consulta de erros
✅ Maior confiabilidade (99.7% uptime)

O QUE NÃO MUDA:

- Suas atividades diárias no sistema RH
- Processos de admissão/demissão
- Envio para o portal eSocial

JANELA DE MANUTENÇÃO:
Data: Sábado, [DATA]
Horário: 02:00 - 06:00
Impacto: Processamento pausado durante a janela

TREINAMENTO:
Link da gravação: [URL]
Manual do usuário: [URL]
FAQ: [URL]

SUPORTE:
Email: suporte-esocial@empresa.com
Telefone: (11) XXXX-XXXX (24x7)
Slack: \#suporte-esocial

Atenciosamente,
Time de Tecnologia

```

---

## Contingências

### Cenários de Contingência

#### Cenário 1: Certificado Digital Inválido

**Sintoma:** Erro ao enviar para eSocial (401 Unauthorized)

**Impacto:** CRÍTICO (bloqueio total)

**Solução:**
1. Verificar validade do certificado (not after)
2. Renovar certificado (A1 ou A3)
3. Reinstalar no servidor (keystore)
4. Reiniciar serviços

**Tempo de Resolução:** 30 minutos (se certificado disponível)

**Prevenção:** Alertas de expiração (30 dias de antecedência)

---

#### Cenário 2: Cluster Kafka Indisponível

**Sintoma:** Producer não consegue publicar eventos

**Impacto:** ALTO (acúmulo de eventos)

**Solução:**
1. Verificar saúde dos brokers (`kafka-broker-api-versions`)
2. Reiniciar broker problemático
3. Se cluster todo down: verificar Zookeeper
4. Failover para DR cluster (se disponível)

**Tempo de Resolução:** 15 minutos

**Prevenção:** Monitoramento contínuo + alertas

---

#### Cenário 3: PostgreSQL com Performance Degradada

**Sintoma:** Persistência lenta (P95 > 500ms)

**Impacto:** MÉDIO (consumer lag)

**Solução:**
1. Verificar queries lentas (`pg_stat_statements`)
2. Criar índices faltantes
3. Ajustar connection pool (HikariCP)
4. Escalar verticalmente (se necessário)

**Tempo de Resolução:** 1 hora

**Prevenção:** Testes de carga + índices bem planejados

---

#### Cenário 4: Taxa de Erro Elevada (> 15%)

**Sintoma:** Muitos eventos na DLQ

**Impacto:** MÉDIO (dados não processados)

**Solução:**
1. Analisar erros (`GET /api/v1/validation/errors`)
2. Identificar regra problemática
3. Ajustar severidade (ERROR → WARNING) se aplicável
4. Corrigir dados na origem
5. Reprocessar DLQ

**Tempo de Resolução:** 2 horas

**Prevenção:** Validação rigorosa em homologação

---

## Aprovações Finais

### Comitê de Aprovação

| Papel | Nome | Data | Assinatura | Decisão |
|-------|------|------|------------|---------|
| **CTO** | [Nome] | [DATA] | ⬜ | GO / NO-GO |
| **Tech Lead** | Márcio Kuroki | [DATA] | ⬜ | GO / NO-GO |
| **Gerente RH** | [Nome] | [DATA] | ⬜ | GO / NO-GO |
| **Gerente Infra** | [Nome] | [DATA] | ⬜ | GO / NO-GO |
| **Compliance** | [Nome] | [DATA] | ⬜ | GO / NO-GO |
| **Segurança** | [Nome] | [DATA] | ⬜ | GO / NO-GO |

**Critério:** Unanimidade para GO (todos devem aprovar).

---

## Anexos

### Anexo A: Scripts de Validação

```

\#!/bin/bash

# scripts/validate-production.sh

echo "Validando ambiente de produção..."

# 1. Health checks

echo "1. Health checks..."
curl -s http://producer-service:8081/actuator/health | jq '.status'
curl -s http://consumer-service:8082/actuator/health | jq '.status'

# 2. Kafka

echo "2. Verificando Kafka..."
kafka-broker-api-versions --bootstrap-server localhost:9092 | wc -l

# Deve retornar 3 (3 brokers)

# 3. PostgreSQL

echo "3. Verificando PostgreSQL..."
psql -U esocial_user -d esocial -c "SELECT 1"

# 4. Certificado Digital

echo "4. Verificando certificado..."
keytool -list -keystore /etc/ssl/certs/esocial-cert.jks

# 5. Métricas

echo "5. Verificando métricas..."
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.health == "up") | .labels.job'

echo "Validação concluída!"

```

---

### Anexo B: Contatos de Emergência

| Papel | Nome | Celular | Email |
|-------|------|---------|-------|
| **Tech Lead** | Márcio Kuroki | - | marciokuroki@gmail.com |

---

## Changelog

| Versão | Data | Autor | Mudanças |
|--------|------|-------|----------|
| 1.0 | 2025-11-22 | Márcio Kuroki | Criação inicial |

---

**Última atualização:** 2025-11-22
**Responsável:** Márcio Kuroki Gonçalves