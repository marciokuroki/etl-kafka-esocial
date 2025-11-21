# Testes Unitários e de Integração

## Execução local

Para rodar todos os testes unitários:
```
mvn clean test
```

Para rodar testes de integração (perfil `integration-tests`):
```
mvn clean verify -Pintegration-tests
```

## Configuração

- Testcontainers para Kafka e Postgres usados para testes isolados.
- Testes de integração no diretório `src/integration-test/java`.
- Nomeação padrão das classes: `*IT.java`

## Pipeline CI/CD

- Testes executados automaticamente no GitHub Actions antes do deploy.
- Falha em testes aborta o deploy.

## Como adicionar novos testes

- Criar testes unitários para lógica isolada.
- Criar testes de integração para fluxo completo: consumo Kafka, validação, persistência.
