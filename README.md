# Pipeline ETL eSocial com Apache Kafka

[![Status](https://img.shields.io/badge/status-Sprint%201%20Complete-brightgreen)]()
[![Build](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Coverage](https://img.shields.io/badge/coverage-82%25-brightgreen)]()
[![License](https://img.shields.io/badge/license-TCC-blue)]()

Solução de streaming de dados event-driven para integração com o eSocial utilizando Apache Kafka, Spring Boot e PostgreSQL.

## 📋 Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Status do Projeto](#status-do-projeto)
- [Arquitetura](#arquitetura)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Instalação](#instalação)
- [Uso](#uso)
- [Testes](#testes)
- [Documentação](#documentação)
- [Roadmap](#roadmap)
- [Contribuição](#contribuição)
- [Licença](#licença)
- [Contato](#contato)

---

## 📖 Sobre o Projeto

O **Pipeline ETL eSocial** é uma solução completa de streaming de dados que captura mudanças em sistemas de RH, valida conforme regras do eSocial e prepara dados para envio ao portal governamental.

### Contexto

O eSocial é um sistema do governo federal que unifica a prestação de informações trabalhistas. Este projeto implementa um pipeline ETL robusto e escalável para automatizar o envio de dados ao eSocial, garantindo:

- ✅ **Conformidade** com regras do eSocial
- ✅ **Rastreabilidade** completa (audit trail)
- ✅ **Escalabilidade** horizontal
- ✅ **Resiliência** (zero perda de dados)
- ✅ **Observabilidade** em tempo real

### Propósito Acadêmico

Este projeto faz parte do Trabalho de Conclusão de Curso (TCC) da Pós-Graduação em Arquitetura de Software e Soluções pela XP Educação.

**Orientador:** Reinaldo Galvão  
**Aluno:** Márcio Kuroki Gonçalves  
**Ano:** 2025

---

## 🚀 Status do Projeto

### Sprint 1 - ✅ Concluída (100%)

**Período:** 01/11/2025 - 30/11/2025

| Serviço | Status | Build | Testes | Coverage | Funcionalidades |
|---------|--------|-------|--------|----------|-----------------|
| **Producer Service** | ✅ Completo | ✅ Passing | 18/18 | 82% | CDC + Kafka Producer |
| **Consumer Service** | ✅ Completo | ✅ Passing | - | - | Validation + Persistence + API |
| **Kafka Cluster** | ✅ Operacional | - | - | - | 3 brokers, 4 topics |
| **PostgreSQL** | ✅ Configurado | - | - | - | Origem + Destino + Audit |
| **Observabilidade** | ✅ Funcionando | - | - | - | Prometheus + Grafana |
| **Documentação** | ✅ Completa | - | - | - | C4 Model + 5 ADRs |

### Próximas Sprints

- 🔄 **Sprint 2** (em planejamento): Testes + Dashboards + Swagger
- 📋 **Sprint 3** (backlog): CI/CD + Produção

---

## 🏗️ Arquitetura

### Visão Geral

```

┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  Sistema RH │ --> │ Producer Svc │ --> │   Kafka     │
│  (Origem)   │     │  (CDC+Pub)   │     │  (Broker)   │
└─────────────┘     └──────────────┘     └──────┬──────┘
                                                │
                                                ▼
                                         ┌──────────────┐
                                         │ Consumer Svc │
                                         │ (Valid+Pers) │
                                         └──────┬───────┘
                                                │
                                                ▼
                                         ┌──────────────┐
                                         │ PostgreSQL   │
                                         │ (Destino)    │
                                         └──────────────┘
```

### Componentes

| Componente | Tecnologia | Porta | Função |
|------------|-----------|-------|--------|
| Producer Service | Spring Boot 3.2 | 8081 | Change Data Capture + Publicação Kafka |
| Consumer Service | Spring Boot 3.2 | 8082 | Consumo + Validação + Persistência + API |
| Kafka Cluster | Confluent 7.5 | 9092-9094 | Message Broker (3 brokers) |
| Zookeeper | Apache 3.8 | 2181 | Coordenação Kafka |
| PostgreSQL Origem | PostgreSQL 15 | 5432 | Sistema legado (simulado) |
| PostgreSQL Destino | PostgreSQL 15 | 5432 | Dados processados |
| Prometheus | Prometheus 2.45 | 9090 | Coleta de métricas |
| Grafana | Grafana 10.0 | 3000 | Dashboards |
| Kafka UI | Provectus | 8090 | Interface Kafka |
| PgAdmin | PgAdmin 4 | 5050 | Admin PostgreSQL |

**Documentação completa:** [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## ✨ Funcionalidades

### ✅ Implementado (Sprint 1)

#### Producer Service
- [x] Change Data Capture via polling (5s)
- [x] Detecção automática de tipo de evento (CREATE/UPDATE/DELETE)
- [x] Publicação em tópicos Kafka separados por tipo
- [x] Métricas Prometheus
- [x] Health checks
- [x] 18 testes unitários (82% coverage)

#### Consumer Service
- [x] Consumo de eventos Kafka (3 tópicos)
- [x] Validação em 2 camadas (estrutural + negócio)
  - [x] 6 regras estruturais (formato, obrigatoriedade)
  - [x] 5 regras de negócio (idade, datas, salário)
- [x] Persistência com versionamento
- [x] Audit trail completo
- [x] Dead Letter Queue (DLQ)
- [x] API REST para relatórios
  - [x] `GET /api/v1/validation/errors` - Erros de validação
  - [x] `GET /api/v1/validation/dashboard` - Dashboard
  - [x] `GET /api/v1/validation/dlq` - Eventos DLQ
- [x] Métricas Prometheus
- [x] Health checks

#### Infraestrutura
- [x] Cluster Kafka (3 brokers, RF=3)
- [x] 4 tópicos com 3 partições cada
- [x] PostgreSQL com schemas separados (source, public, audit)
- [x] Stack de observabilidade completa
- [x] Docker Compose (14 containers)
- [x] Scripts de automação

#### Documentação
- [x] Arquitetura C4 Model (3 níveis)
- [x] 5 ADRs (Architectural Decision Records)
- [x] READMEs técnicos (Producer e Consumer)
- [x] Guias de setup e troubleshooting

### 🔄 Roadmap (Próximas Sprints)

#### Sprint 2 - Qualidade e Observabilidade
- [ ] Testes de integração (Testcontainers)
- [ ] Testes de carga (JMeter)
- [ ] Dashboards Grafana customizados
- [ ] Alertas Prometheus
- [ ] Documentação Swagger/OpenAPI
- [ ] Testes unitários Consumer (35+ testes)

#### Sprint 3 - Produção
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Migração CDC para Debezium
- [ ] Segurança (TLS, SASL)
- [ ] Backup e recuperação
- [ ] Documentação de deployment
- [ ] Testes end-to-end

---

## 🛠️ Tecnologias

### Backend
- **Java 21** - Linguagem
- **Spring Boot 3.2.0** - Framework
- **Spring Kafka 3.1.0** - Integração Kafka
- **Spring Data JPA** - Persistência
- **Lombok** - Redução de boilerplate

### Message Broker
- **Apache Kafka 3.5** - Message broker
- **Zookeeper 3.8** - Coordenação

### Banco de Dados
- **PostgreSQL 15** - Banco relacional
- **HikariCP** - Connection pooling

### Observabilidade
- **Prometheus 2.45** - Métricas
- **Grafana 10.0** - Dashboards
- **Micrometer** - API de métricas
- **SLF4J + Logback** - Logs

### DevOps
- **Docker** - Containerização
- **Docker Compose** - Orquestração local
- **Maven 3.9** - Build

### Testes
- **JUnit 5** - Framework de testes
- **Mockito** - Mocking
- **AssertJ** - Assertions fluentes
- **JaCoCo** - Cobertura de código
- **H2 Database** - Banco in-memory (testes)

### Ferramentas
- **Kafka UI** - Interface Kafka
- **PgAdmin 4** - Admin PostgreSQL
- **IntelliJ IDEA** - IDE

---

## 📋 Pré-requisitos

### Obrigatórios
- **Docker** 24.0+ e **Docker Compose** 2.20+
- **Git** 2.40+
- **8GB RAM** mínimo (16GB recomendado)
- **20GB** de espaço em disco

### Opcionais (para desenvolvimento)
- **Java 21** (OpenJDK ou Oracle JDK)
- **Maven 3.9+**
- **IntelliJ IDEA** ou **VS Code**

### Verificar Instalação

```


# Docker

docker --version

# Saída esperada: Docker version 24.0.x

# Docker Compose

docker-compose --version

# Saída esperada: Docker Compose version 2.20.x

# Git

git --version

# Saída esperada: git version 2.40.x

```

---

## 🚀 Instalação

### 1. Clonar o Repositório

```

git clone https://github.com/seu-usuario/etl-kafka-esocial.git
cd etl-kafka-esocial

```

### 2. Configurar Variáveis de Ambiente (Opcional)

```


# Copiar arquivo de exemplo

cp .env.example .env

# Editar conforme necessário

vim .env

```

### 3. Compilar os Serviços (Opcional)

```


# Se quiser fazer alterações no código

cd producer-service \&\& mvn clean package -DskipTests
cd ../consumer-service \&\& mvn clean package -DskipTests
cd ..

```

### 4. Iniciar Todos os Containers

```


# Iniciar infraestrutura completa

docker-compose up -d

# Aguardar containers ficarem healthy (~2 minutos)

docker-compose ps

# Ver logs em tempo real

docker-compose logs -f producer-service consumer-service

```

### 5. Validar Instalação

```


# Health checks

curl http://localhost:8081/actuator/health | jq
curl http://localhost:8082/actuator/health | jq

# Acessar interfaces

# Kafka UI: http://localhost:8090

# Prometheus: http://localhost:9090

# Grafana: http://localhost:3000 (admin/admin)

```

**Status esperado:** Todos os serviços retornam `{"status":"UP"}`

---

## 💻 Uso

### Cenário 1: Inserir Novo Colaborador

```


# 1. Conectar no PostgreSQL

docker exec -it esocial-postgres-db psql -U esocial_user -d esocial

# 2. Inserir colaborador

INSERT INTO source.employees VALUES (
'EMP100',
'12345678901',
'10011223344',
'João da Silva Santos',
'1990-01-15',
'2024-01-10',
NULL,
'Analista de Sistemas',
'TI',
5500.00,
'ACTIVE',
NOW(),
NOW()
);

# 3. Aguardar 5 segundos (polling)

# 4. Verificar processamento

SELECT * FROM public.employees WHERE source_id = 'EMP100';
SELECT * FROM audit.employees_history WHERE source_id = 'EMP100';

```

**Resultado esperado:**
- ✅ Producer captura mudança
- ✅ Evento publicado no Kafka (topic: employee-create)
- ✅ Consumer valida dados
- ✅ Registro persistido no destino
- ✅ Histórico criado na audit

### Cenário 2: Atualizar Salário

```

-- Atualizar salário
UPDATE source.employees
SET salary = 6500.00, updated_at = NOW()
WHERE employee_id = 'EMP100';

-- Verificar versionamento
SELECT source_id, salary, version FROM public.employees
WHERE source_id = 'EMP100';

-- Ver histórico
SELECT operation, salary, version, changed_at
FROM audit.employees_history
WHERE source_id = 'EMP100'
ORDER BY changed_at;

```

**Resultado esperado:**
- ✅ Version incrementada (1 → 2)
- ✅ Histórico com 2 registros (INSERT + UPDATE)

### Cenário 3: Consultar Erros de Validação

```


# API REST

curl http://localhost:8082/api/v1/validation/errors | jq

# Dashboard

curl http://localhost:8082/api/v1/validation/dashboard | jq

# Dead Letter Queue

curl http://localhost:8082/api/v1/validation/dlq | jq

```

### Cenário 4: Monitorar Métricas

```


# Métricas do Producer

curl http://localhost:8081/actuator/prometheus | grep events_published

# Métricas do Consumer

curl http://localhost:8082/actuator/prometheus | grep events_consumed

# Ou acessar dashboards

open http://localhost:9090  \# Prometheus
open http://localhost:3000  \# Grafana

```

---

## 🧪 Testes

### Executar Testes do Producer

```

cd producer-service

# Todos os testes

mvn test

# Com relatório de cobertura

mvn clean test

# Ver relatório HTML

open target/site/jacoco/index.html

# Teste específico

mvn test -Dtest=KafkaProducerServiceTest

```

**Resultado esperado:**
```

Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
Coverage: 82%

```

### Executar Testes do Consumer (Sprint 2)

```

cd consumer-service
mvn test

```

### Testes de Integração (Sprint 2)

```


# Testes end-to-end

mvn verify -Pintegration-tests

```

---

## 📚 Documentação

### Documentação Técnica

| Documento | Descrição | Link |
|-----------|-----------|------|
| **Arquitetura** | C4 Model completo (3 níveis) | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| **ADRs** | Decisões arquiteturais (5 ADRs) | [docs/adr/](docs/adr/) |
| **Producer Service** | README técnico | [producer-service/README.md](producer-service/README.md) |
| **Consumer Service** | README técnico | [consumer-service/README.md](consumer-service/README.md) |
| **Testes - Producer** | Guia de testes | [producer-service/TESTING.md](producer-service/TESTING.md) |
| **Sprint 1** | Retrospectiva e evidências | [docs/sprint1/](docs/sprint1/) |

### Guias de Setup

- [Setup Docker Compose](docs/sprint1/setup/docker-compose-setup.md)
- [Setup Kafka Cluster](docs/sprint1/setup/kafka-cluster-setup.md)
- [Setup PostgreSQL](docs/sprint1/setup/postgres-setup.md)
- [Troubleshooting](docs/sprint1/lessons-learned/)

### APIs REST

#### Producer Service
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Métricas
- `GET /actuator/info` - Informações da aplicação

#### Consumer Service
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Métricas
- `GET /api/v1/validation/errors` - Lista erros de validação
- `GET /api/v1/validation/dashboard` - Dashboard com estatísticas
- `GET /api/v1/validation/dlq` - Eventos na Dead Letter Queue
- `POST /api/v1/validation/dlq/{id}/retry` - Reprocessar evento DLQ

**Documentação Swagger:** (Sprint 2)

---

## 🗺️ Roadmap

### ✅ Sprint 1 - Infraestrutura Base (Concluída)
- [x] Setup Docker Compose
- [x] Cluster Kafka (3 brokers)
- [x] Producer Service (CDC + Kafka)
- [x] Consumer Service (Validation + Persistence)
- [x] Observabilidade (Prometheus + Grafana)
- [x] Documentação (C4 + ADRs)

### 🔄 Sprint 2 - Qualidade e Monitoramento (Em Planejamento)
- [ ] Testes unitários Consumer (35+ testes)
- [ ] Testes de integração (Testcontainers)
- [ ] Testes de carga (JMeter)
- [ ] Dashboards Grafana
- [ ] Alertas Prometheus
- [ ] Documentação Swagger

### 📋 Sprint 3 - Produção (Backlog)
- [ ] CI/CD (GitHub Actions)
- [ ] Migração CDC (Debezium)
- [ ] Segurança (TLS + SASL)
- [ ] Backup e DR
- [ ] Documentação deployment
- [ ] Testes E2E

---

## 🤝 Contribuição

Este é um projeto aplicado, mas sugestões são bem-vindas!

### Como Contribuir

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'Add: nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

### Padrão de Commits

Seguimos [Conventional Commits](https://www.conventionalcommits.org/):

```

feat: adicionar nova funcionalidade
fix: corrigir bug
docs: atualizar documentação
test: adicionar testes
refactor: refatorar código
chore: tarefas de manutenção

```

---
## 📧 Contato

**Aluno:** Márcio Kuroki Gonçalves  
**Email:** [marciokuroki@gmail.com]  
**GitHub:** [github.com/marciokuroki]

**Orientador:** Reinaldo Galvão  
**Instituição:** XP Educação  
**Curso:** Pós-Graduação em Arquitetura de Software e Soluções

---

## 📊 Estatísticas do Projeto

| Métrica | Valor |
|---------|-------|
| Linhas de Código | ~8.000 |
| Testes Unitários | 18 (Producer) + 35 (Consumer - Sprint 2) |
| Cobertura | 82% |
| Containers | 14 |
| Serviços Spring Boot | 2 |
| ADRs Documentados | 5 |
| Duração Sprint 1 | 1 semana |
| Commits | 150+ |