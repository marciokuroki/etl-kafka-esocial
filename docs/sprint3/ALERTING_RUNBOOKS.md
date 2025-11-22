# Runbooks - Sistema de Alertas

## HighErrorRate - Taxa de Erro Elevada

### Descrição
Taxa de validação com falha superior a 5% nos últimos 5 minutos.

### Causa Provável
- Dados de origem com problemas de qualidade
- Mudança no formato dos dados sem atualização das validações
- Bug nas regras de validação

### Diagnóstico
1. Consultar erros recentes:
```
curl http://localhost:8082/api/v1/validation/errors | jq
```
2. Verificar distribuição por tipo:
```
curl http://localhost:8082/api/v1/validation/errors/stats | jq
```
3. Analisar logs:
```
docker logs esocial-consumer-service --tail 100 | grep ERROR
```

### Resolução
- **Se erro em massa (mesmo tipo):** Corrigir dados na origem ou atualizar validação
- **Se erros pontuais:** Investigar casos individualmente via DLQ

### Prevenção
- Implementar validação na origem antes do CDC
- Adicionar testes de regressão para regras de validação

---

## HighLatency - Latência Elevada

### Descrição
Latência P95 de processamento superior a 10 segundos.

### Causa Provável
- Carga alta no sistema
- Queries lentas no PostgreSQL
- Problemas de rede com Kafka

### Diagnóstico
1. Verificar carga:
```
docker stats --no-stream
```
2. Analisar métricas Kafka:
- Acessar http://localhost:8090
- Verificar latência por partição

3. Queries PostgreSQL:
```
SELECT * FROM pg_stat_activity WHERE state = 'active';
```

### Resolução
- **Alta carga:** Escalar horizontalmente (mais consumidores)
- **Query lenta:** Otimizar índices ou consultas
- **Kafka lento:** Verificar replication factor e configurações

---

## HighDLQCount - Muitos Eventos na DLQ

### Descrição
Mais de 100 eventos acumulados na Dead Letter Queue.

### Diagnóstico
```
curl http://localhost:8082/api/v1/validation/dlq | jq '.[] | {id, errorMessage, retryCount}'
```

### Resolução
1. Identificar padrão de erro comum
2. Corrigir causa raiz
3. Reprocessar eventos:
```
curl -X POST http://localhost:8082/api/v1/validation/dlq/{id}/retry
```

---

## ServiceDown - Serviço Indisponível

### Diagnóstico
```
docker ps -a | grep producer-service
docker logs esocial-producer-service --tail 50
```

### Resolução Imediata
```
docker-compose restart producer-service
```

### Investigação
- Verificar logs de erro
- Analisar recursos (memória, CPU)
- Validar conectividade com dependências

