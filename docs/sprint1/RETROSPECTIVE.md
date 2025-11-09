# Retrospectiva Sprint 1

**Data:** 30/11/2025  
**Participantes:** Equipe de desenvolvimento  
**Facilitador:** Márcio Kuroki

## 🎯 Objetivos da Sprint

- [x] Criar infraestrutura base com Kafka
- [x] Implementar Producer Service
- [x] Implementar Consumer Service
- [x] Configurar observabilidade
- [x] Documentar arquitetura

## ⭐ O que funcionou bem (Keep)

### 1. Docker Compose
**Voto:** ⭐⭐⭐⭐⭐  
Containerização de todos os serviços facilitou enormemente o desenvolvimento e garantiu consistência entre ambientes.

**Evidências:**
- Zero problemas de "funciona na minha máquina"
- Setup completo em 5 minutos
- 14 containers rodando harmonicamente

**Ação:** Continuar expandindo docker-compose nas próximas sprints.

### 2. Métricas desde o início
**Voto:** ⭐⭐⭐⭐  
Prometheus + Grafana desde o começo permitiu identificar problemas de performance rapidamente.

**Evidências:**
- Latência P95 monitorada desde o primeiro deploy
- 3 problemas de performance identificados precocemente
- Dashboards prontos para demonstração

### 3. Testes Unitários
**Voto:** ⭐⭐⭐⭐  
18 testes do Producer pegaram 4 bugs antes de ir para integração.

**Evidências:**
- Cobertura de 82% (acima da meta de 70%)
- 4 bugs encontrados e corrigidos antes de integração
- Refactoring com confiança

## 🔄 O que pode melhorar (Improve)

### 1. Testes de Integração ausentes
**Impacto:** Médio  
**Problema:** Falta de testes end-to-end completos.

**Ações:**
- [ ] Priorizar testes de integração na Sprint 2
- [ ] Criar suite de testes com Testcontainers
- [ ] Automatizar no CI/CD

### 2. Documentação tardia
**Impacto:** Baixo  
**Problema:** ADRs criados ao final da sprint.

**Ações:**
- [ ] Documentar decisões no momento em que são tomadas
- [ ] Template de ADR no repositório
- [ ] Reminder no DoD dos cards

### 3. Falta de alertas
**Impacto:** Médio  
**Problema:** Métricas coletadas mas sem alertas configurados.

**Ações:**
- [ ] Configurar alertas Prometheus (Sprint 2)
- [ ] Definir SLOs e SLIs
- [ ] Integrar com Slack/email

## 🚫 O que não funcionou (Drop)

### 1. Oracle XE na POC
**Decisão:** Remover Oracle, manter PostgreSQL  
**Justificativa:** Complexidade não justificada para POC

**Ação:** Manter PostgreSQL como padrão, avaliar Oracle apenas em produção.

### 2. Polling interval muito frequente (2s)
**Decisão:** Aumentar para 5s  
**Justificativa:** 2s causava carga desnecessária no banco

**Evidência:** CPU do PostgreSQL caiu de 15% para 8% após ajuste.

## 📊 Métricas da Sprint

### Velocidade
- Story Points planejados: 21
- Story Points entregues: 21
- Velocidade: 21 SP/sprint

### Qualidade
- Bugs encontrados: 7
- Bugs resolvidos: 7
- Bugs em produção: 0
- Cobertura de testes: 82%

### Eficiência
- Lead time médio: 2,8 dias
- Cycle time médio: 1,5 dias
- Tempo de revisão: 0,3 dias

## 🎬 Action Items

| Ação | Responsável | Prazo | Status |
|------|-------------|-------|--------|
| Criar testes de integração | Márcio | Sprint 2 | 🔄 Todo |
| Configurar alertas Prometheus | Márcio | Sprint 2 | 🔄 Todo |
| Documentar troubleshooting | Márcio | Sprint 2 | 🔄 Todo |
| Criar dashboards Grafana | Márcio | Sprint 2 | 🔄 Todo |
| Implementar CI/CD básico | Márcio | Sprint 3 | 📋 Backlog |

## 🏆 Celebrações

- ✨ Zero downtime durante toda a sprint
- ✨ 100% dos story points entregues
- ✨ Cobertura de testes superou meta (82% vs 70%)
- ✨ Latência P95 melhor que esperado (100ms vs 150ms)
- ✨ Documentação arquitetural completa

## 💡 Insights

### Técnicos
1. Kafka é mais fácil de operar do que esperávamos
2. Polling CDC é suficiente para volumes moderados
3. Validação em camadas facilita manutenção

### Processo
1. Daily standups de 10min são suficientes
2. Code review ajuda muito na qualidade
3. Documentar decisões (ADR) evita retrabalho

### Pessoal
1. Prazos apertados mas realistas
2. Trabalho solo requer disciplina maior
3. Ferramentas certas fazem diferença enorme
