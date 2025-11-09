# Documentação do Pipeline ETL eSocial

Bem-vindo à documentação técnica do Pipeline ETL eSocial com Apache Kafka.

## 📚 Índice

### Documentação de Arquitetura

- **[ARCHITECTURE.md](architecture/ARCHITECTURE.md)** - Documentação completa da arquitetura (C4 Model)
  - Nível 1: Diagrama de Contexto
  - Nível 2: Diagrama de Containers
  - Nível 3: Diagrama de Componentes
  - Fluxos de Dados
  - Matriz de Responsabilidades

### Decisões Arquiteturais (ADRs)

Ver pasta [adr/](architecture/ADRs/) para todos os ADRs. Principais decisões:

| # | Título | Status | Data | Descrição |
|---|--------|--------|------|-----------|
| [0001](architecture/ADRs/0001-use-apache-kafka.md) | Uso do Apache Kafka | Aceito | 2025-11-01 | Message broker para transporte de eventos |
| [0002](architecture/ADRs/0002-cdc-via-polling.md) | CDC via Polling | Aceito | 2025-11-02 | Captura de mudanças no banco de origem |
| [0003](architecture/ADRs/0003-two-layer-validation.md) | Validação em Duas Camadas | Aceito | 2025-11-03 | Estrutural + Negócio |
| [0004](architecture/ADRs/0004-audit-trail.md) | Audit Trail Completo | Aceito | 2025-11-04 | Histórico de todas as operações |
| [0005](architecture/ADRs/0005-dead-letter-queue.md) | Dead Letter Queue | Aceito | 2025-11-05 | Tratamento de eventos com falha |

### Documentação Técnica dos Serviços

- **Producer Service**
  - [README.md](../producer-service/README.md) - Visão geral e configuração
  - [TESTING.md](../producer-service/TESTING.md) - Guia de testes unitários
  
- **Consumer Service**
  - [README.md](../consumer-service/README.md) - Visão geral e configuração
  - [TESTING.md](../consumer-service/TESTING.md) - Guia de testes unitários

### Guias e Tutoriais

Documentos PDF disponíveis (ver raiz do projeto):

- `Guia-Completo-Setup-Docker-Compose-POC.pdf` - Setup completo do ambiente
- `Card-1.2-Provisionamento-Cluster-Kafka.pdf` - Configuração do Kafka
- `Relatorio-do-Projeto-Aplicado-Marcio-Kuroki-Goncalves-2025.pdf` - Relatório final

## 🚀 Quick Start

### Para Desenvolvedores

1. **Entender a arquitetura:** Leia [ARCHITECTURE.md](ARCHITECTURE.md)
2. **Entender decisões:** Leia ADRs em [adr/](adr/)
3. **Setup local:** Siga [README.md principal](../README.md)
4. **Rodar testes:** Veja `TESTING.md` de cada serviço

### Para Revisores/Avaliadores

1. **Visão geral:** [README.md principal](../README.md)
2. **Arquitetura:** [ARCHITECTURE.md](ARCHITECTURE.md)
3. **Decisões técnicas:** [ADRs](adr/)
4. **Relatório completo:** `Relatorio-do-Projeto-Aplicado-*.pdf`

## 📊 Métricas do Projeto

| Métrica | Valor |
|---------|-------|
| Linhas de Código | ~8.000 |
| Testes Unitários | 35+ |
| Cobertura de Testes | 80%+ |
| ADRs Documentados | 5 |
| Componentes | 14 containers |
| Duração Sprint 1 | 4 semanas |

## 🔄 Processo de Atualização

### Como Adicionar Novo ADR

1. Copie `adr/template.md` para `adr/000X-titulo.md`
2. Preencha todas as seções
3. Adicione à tabela em `adr/README.md`
4. Crie PR para revisão

### Como Atualizar Arquitetura

1. Edite `ARCHITECTURE.md`
2. Atualize diagramas se necessário
3. Documente mudanças no commit

## 📧 Contato

**Autor:** Márcio Kuroki Gonçalves  
**Instituição:** XP Educação
**Orientador:** Reinaldo Galvão
**Ano:** 2025

## 📄 Licença

Este projeto é parte do Projeto Aplicado da Pós-Graduação em Arquitetura de Software e Arquiteto de Soluções.
Todos os direitos reservados.