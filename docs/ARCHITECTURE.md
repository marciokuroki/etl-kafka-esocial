# Arquitetura do Sistema - Pipeline ETL eSocial

**Modelo:** C4 (Context, Containers, Components, Code)  
**Versão:** 1.0.0  
**Data:** 09/11/2025  
**Autor:** Márcio Kuroki Gonçalves

---

## Índice

1. [Visão Geral](#visão-geral)
2. [Nível 1 - Contexto](#nível-1---contexto)
3. [Nível 2 - Containers](#nível-2---containers)
4. [Nível 3 - Componentes](#nível-3---componentes)
5. [Fluxos de Dados](#fluxos-de-dados)
6. [Matriz de Responsabilidades](#matriz-de-responsabilidades)
7. [Decisões Arquiteturais](#decisões-arquiteturais)
8. [Segurança e Escalabilidade](#segurança-e-escalabilidade)

---

## Visão Geral

O Pipeline ETL eSocial é um sistema distribuído event-driven que:
- Captura mudanças de dados do sistema legado de RH
- Valida conforme regras do eSocial
- Persiste dados preparados para envio ao governo
- Mantém audit trail completo para compliance

**Tecnologias Principais:**
- Apache Kafka (message broker)
- Spring Boot (microsserviços)
- PostgreSQL (persistência)
- Prometheus + Grafana (observabilidade)

---

## Nível 1 - Contexto

### Diagrama de Contexto

```
              ┌──────────────────┐
              │  Analista de RH  │
              │     [Pessoa]     │
              └────────┬─────────┘
                       │ Consulta
                       │ relatórios
                       ▼
      ┌────────────────────────────────┐
      │   Pipeline ETL eSocial         │
      │   [Sistema de Software]        │
      │                                │
      │ Captura, valida e prepara      │
      │ dados trabalhistas             │
      └──┬─────────────────────────┬───┘
         │                         │
         │ Captura                 │ Envia
         │                         │
         ▼                         ▼
    ┌────────────────┐        ┌────────────────┐
    │ Sistema de RH  │        │ Portal eSocial │
    │    Legado      │        │  [Externo]     │
    │                │        │                │
    │  PostgreSQL    │        │  Governo       │
    └────────────────┘        └────────────────┘
```

### Atores e Sistemas Externos

| Elemento | Tipo | Responsabilidade |
|----------|------|------------------|
| **Analista de RH** | Pessoa | Monitora validações, corrige erros |
| **Sistema de RH Legado** | Sistema Externo | Fonte de dados de colaboradores |
| **Portal eSocial** | Sistema Externo | Recebe eventos trabalhistas |
| **Pipeline ETL eSocial** | Sistema Alvo | Captura, valida e prepara dados |

---

## Nível 2 - Containers

### Diagrama de Containers

```
                    ┌──────────────┐
                    │ Analista RH  │
                    └──────┬───────┘
                           │ HTTPS
                           ▼
            ┌──────────────────────────┐
            │      Grafana             │
            │   [Dashboard - 3000]     │
            └──────────┬───────────────┘
                       │ HTTP
                       ▼
    ┌──────────────────────────────────────────────────┐
    │         Apache Kafka Cluster                     │
    │         [Message Broker]                         │
    │                                                  │
    │  ┌───────────┐ ┌───────────┐ ┌───────────┐       │
    │  │ Broker 1  │ │ Broker 2  │ │ Broker 3  │       │
    │  │  :9092    │ │  :9093    │ │  :9094    │       │
    │  └───────────┘ └───────────┘ └───────────┘       │
    │                                                  │
    │  Topics: employee-create, employee-update,       │
    │          employee-delete, esocial-dlq            │
    └──────▲─────────────────────────────┬─────────────┘
           │ Publica                     │ Consome
           │                             │
    ┌──────┴────────┐           ┌────────▼─────────┐
    │ Producer      │           │  Consumer        │
    │ Service       │           │  Service         │
    │ [Spring Boot] │           │  [Spring Boot]   │
    │   :8081       │           │    :8082         │
    │               │           │                  │
    │ - CDC         │           │ - Validation     │
    │ - Kafka Prod  │           │ - Persistence    │
    └───────┬───────┘           └────────┬─────────┘
            │                            │
            │ SELECT                     │ INSERT/UPDATE
            ▼                            ▼
     ┌───────────────┐          ┌────────────────────┐
     │ PostgreSQL    │          │  PostgreSQL        │
     │ (Origem)      │          │  (Destino)         │
     │   :5432       │          │    :5432           │
     │               │          │                    │
     │ source.       │          │ public.employees   │
     │  employees    │          │ validation_errors  │
     │               │          │ dlq_events         │
     │               │          │ audit.history      │
     └───────────────┘          └────────────────────┘

         ┌─────────────────────┐
         │   Prometheus        │
         │   [Metrics - 9090]  │
         └──────┬──────────────┘
                │ Scrape /actuator/prometheus
         ┌──────┴──────┐
         │             │
    Producer:8081  Consumer:8082
```    

### Descrição dos Containers

#### Producer Service (Spring Boot - Porta 8081)
**Responsabilidades:**
- Change Data Capture via polling (5s)
- Publicação de eventos no Kafka
- Determinação de tipo de evento
- Métricas Prometheus

**APIs:**
- `GET /actuator/health`
- `GET /actuator/prometheus`

**Configurações:**
- Polling: 5 segundos
- Batch: 100 registros
- Kafka acks: all

---

#### Consumer Service (Spring Boot - Porta 8082)
**Responsabilidades:**
- Consumo de eventos do Kafka
- Validação (estrutural + negócio)
- Persistência no banco destino
- Audit trail
- Dead Letter Queue
- API REST de relatórios

**APIs:**
- `GET /api/v1/validation/errors`
- `GET /api/v1/validation/dashboard`
- `GET /api/v1/validation/dlq`
- `GET /actuator/health`
- `GET /actuator/prometheus`

**Configurações:**
- Consumer group: esocial-consumer-group
- Concurrency: 3 threads
- ACK: manual

---

#### Apache Kafka Cluster
**Configuração:**
- 3 brokers (HA)
- Replication factor: 3
- Min in-sync replicas: 2
- 4 tópicos (3 partitions cada)

**Tópicos:**
- `employee-create` (CREATE events)
- `employee-update` (UPDATE events)
- `employee-delete` (DELETE events)
- `esocial-dlq` (failed events)

**Documento mantido por:** Márcio Kuroki Gonçalves  
**Última atualização:** 09/11/2025  
**Próxima revisão:** Sprint 3