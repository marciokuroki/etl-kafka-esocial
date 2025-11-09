# Architectural Decision Records (ADRs)

Este diretório contém os registros de decisões arquiteturais do Pipeline ETL eSocial.

## O que é um ADR?

Um ADR documenta uma decisão arquitetural significativa, incluindo:
- O contexto que motivou a decisão
- A decisão tomada
- As alternativas consideradas
- As consequências da decisão

## Formato

Seguimos o formato [MADR](https://adr.github.io/madr/) (Markdown ADR).

## Lista de ADRs

| # | Título | Status | Data |
|---|--------|--------|------|
| [0001](0001-use-apache-kafka.md) | Uso do Apache Kafka | Aceito | 2025-11-01 |
| [0002](0002-cdc-via-polling.md) | CDC via Polling | Aceito | 2025-11-02 |
| [0003](0003-two-layer-validation.md) | Validação em Duas Camadas | Aceito | 2025-11-03 |
| [0004](0004-audit-trail.md) | Audit Trail Completo | Aceito | 2025-11-04 |
| [0005](0005-dead-letter-queue.md) | Dead Letter Queue | Aceito | 2025-11-05 |

## Como criar um novo ADR

1. Copie o template: `cp template.md 000X-titulo-do-adr.md`
2. Preencha as seções
3. Adicione à tabela acima
4. Crie um PR para discussão

## Status Possíveis

- **Proposto** - Em discussão
- **Aceito** - Decisão aprovada e implementada
- **Rejeitado** - Proposta rejeitada
- **Descontinuado** - Substituído por outro ADR
- **Supersedido por [000X]** - Substituído por ADR específico
