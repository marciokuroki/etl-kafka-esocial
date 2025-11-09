# 0005. Dead Letter Queue (DLQ)

**Status:** Aceito  
**Data:** 2025-11-05  
**Decisores:** Márcio Kuroki Gonçalves  
**Tags:** error-handling, resilience, kafka, postgresql

## Contexto e Problema

Durante o processamento de eventos do Kafka, podem ocorrer falhas irrecuperáveis como:
- Erros de validação (CPF inválido, idade menor que 16 anos)
- Erros de formato (JSON malformado)
- Erros de lógica de negócio (datas inconsistentes)
- Erros técnicos (timeout, exceções não tratadas)

**Problema:** Como garantir que nenhum evento seja perdido mesmo quando falhas ocorrem, e permitir que esses eventos sejam analisados e reprocessados posteriormente?

## Fatores de Decisão

* Zero perda de mensagens (requisito crítico)
* Facilidade de análise de erros
* Possibilidade de reprocessamento manual
* Performance do consumer (não bloquear por causa de 1 erro)
* Simplicidade de implementação
* Observabilidade (dashboards, alertas)
* Custo de infraestrutura
* Separação entre erros recuperáveis e irrecuperáveis

## Opções Consideradas

* DLQ em tópico Kafka separado
* DLQ em PostgreSQL (escolhida)
* DLQ em arquivo (filesystem)
* Retry com backoff exponencial + DLQ eventual
* Descartar eventos com erro (❌ não aceitável)

## Decisão

**Escolhido:** Dead Letter Queue em PostgreSQL

**Justificativa:** Armazenar eventos falhados no PostgreSQL oferece melhor capacidade de query e análise comparado a tópico Kafka, com custo de infraestrutura mínimo (banco já existe).

**Arquitetura:**
```

Kafka Event → Consumer → Validation
↓ (se falha)
DLQ (PostgreSQL)
↓
Dashboard / Reprocessamento Manual

```

## Consequências

### Positivas

* ✅ **Zero perda de mensagens**: Eventos com erro são persistidos
* ✅ **Queries SQL**: Fácil consultar erros por tipo, período, campo
* ✅ **Reprocessamento**: Possível reenviar para Kafka manualmente
* ✅ **Dashboard**: Interface web pode consultar diretamente
* ✅ **Análise**: JOIN com outras tabelas para debugging
* ✅ **Custo zero**: Usa PostgreSQL existente
* ✅ **Simplicidade**: Não requer cluster Kafka adicional
* ✅ **Transacional**: INSERT na DLQ é parte da transação

### Negativas

* ❌ **Storage no PostgreSQL**: Aumenta uso do banco
* ❌ **Não é streaming**: Eventos ficam "parados" até ação manual
* ❌ **Reprocessamento manual**: Não há retry automático
* ❌ **Acoplamento**: DLQ depende do banco estar disponível

### Riscos

* **Risco de DLQ crescer indefinidamente**
  - Mitigação: Alertas quando > 1000 eventos, purge após resolução
  
* **Risco de mascarar problemas sistêmicos**
  - Mitigação: Alertas automáticos, análise semanal de erros recorrentes
  
* **Risco de timeout no INSERT da DLQ**
  - Mitigação: Log em arquivo como fallback

## Alternativas

### DLQ em Tópico Kafka

**Descrição:** Criar tópico `esocial-dlq` no Kafka para eventos falhados.

**Prós:**
- ✅ Consistente com arquitetura event-driven
- ✅ Possibilidade de consumer automático para retry
- ✅ Não usa storage do PostgreSQL
- ✅ Padrão da indústria

**Contras:**
- ❌ Difícil fazer queries complexas (não é SQL)
- ❌ Requer ferramentas específicas para análise
- ❌ Kafka UI não é ideal para análise de erros
- ❌ Dificulta JOIN com dados de outras tabelas
- ❌ Retenção limitada (7 dias default)

**Por que foi rejeitada:** Análise de erros via SQL é muito mais produtiva que via ferramentas Kafka. Interface web pode consultar facilmente.

### Retry com Backoff Exponencial

**Descrição:** Tentar reprocessar N vezes com delay crescente antes de DLQ.

**Prós:**
- ✅ Resolve erros transientes automaticamente
- ✅ Menos eventos na DLQ
- ✅ Não requer intervenção manual

**Contras:**
- ❌ Bloqueia consumer durante retry
- ❌ Erros de validação não são transientes
- ❌ Complexidade adicional
- ❌ Dificulta troubleshooting (múltiplos logs)

**Por que foi rejeitada:** Maioria dos erros são de validação (não transientes). Retry não resolveria. Decidido implementar retry **apenas** para erros técnicos (timeout, connection).

**Status:** Considerar em Sprint 3 para erros técnicos específicos.

### DLQ em Arquivo (Filesystem)

**Descrição:** Escrever eventos falhados em arquivo JSON.

**Prós:**
- ✅ Simples
- ✅ Não depende de banco
- ✅ Fácil de fazer backup

**Contras:**
- ❌ Difícil fazer queries
- ❌ Difícil criar dashboard
- ❌ Reprocessamento manual complexo
- ❌ Não escala (milhões de arquivos)

**Por que foi rejeitada:** Não oferece capacidade de query e análise necessárias.

## Validação

A decisão será validada através de:

1. **Teste de falha 100%:**
   - Processar 1000 eventos inválidos
   - Meta: 1000 eventos na DLQ, zero perdidos
   - ✅ **Resultado:** 1000/1000 na DLQ

2. **Teste de reprocessamento:**
   - Corrigir evento na DLQ e reenviar
   - Meta: Processado com sucesso
   - ✅ **Resultado:** Sucesso

3. **Teste de query:**
   - Buscar erros de CPF inválido no último mês
   - Meta: < 500ms
   - ✅ **Resultado:** 180ms

4. **Teste de dashboard:**
   - Carregar estatísticas de DLQ
   - Meta: < 2s
   - ✅ **Resultado:** 1.2s

## Implementação

### Tabela DLQ

```

CREATE TABLE public.dlq_events (
id BIGSERIAL PRIMARY KEY,

    -- Identificação do evento
    event_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    source_table VARCHAR(50),
    source_id VARCHAR(50),
    
    -- Payload completo (para reprocessamento)
    event_payload JSONB NOT NULL,
    
    -- Informações do erro
    error_message TEXT NOT NULL,
    stack_trace TEXT,
    
    -- Controle de retry
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, RESOLVED, DISCARDED
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_retry_at TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(50),
    
    -- Metadados Kafka
    kafka_offset BIGINT,
    kafka_partition INTEGER,
    kafka_topic VARCHAR(100),
    correlation_id UUID
    );

-- Índices
CREATE INDEX idx_dlq_status ON public.dlq_events(status);
CREATE INDEX idx_dlq_created_at ON public.dlq_events(created_at);
CREATE INDEX idx_dlq_source_id ON public.dlq_events(source_id);
CREATE INDEX idx_dlq_event_type ON public.dlq_events(event_type);

```

### Lógica de Envio para DLQ

```

@Service
public class KafkaConsumerService {

    @KafkaListener(topics = {"employee-create", "employee-update", "employee-delete"})
    public void consumeEmployeeEvent(
            @Payload EmployeeEventDTO event,
            @Header(KafkaHeaders.OFFSET) Long offset,
            @Header(KafkaHeaders.PARTITION) Integer partition,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            // Validar
            ValidationResult result = validationService
                .validateAndPersistErrors(event, offset, partition, topic);
            
            if (result.isValid()) {
                // Persistir
                persistenceService.persistEvent(event, offset, partition, topic);
                log.info("Evento processado com sucesso: {}", event.getEventId());
            } else {
                // Enviar para DLQ
                log.warn("Evento inválido, enviando para DLQ: {}", event.getEventId());
                sendToDLQ(event, topic, offset, partition, 
                    "Falha na validação: " + result.getErrorSummary());
            }
            
            // Commit manual (mesmo com erro, não reprocessar)
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Erro ao processar evento: {}", event.getEventId(), e);
            sendToDLQ(event, topic, offset, partition, e.getMessage());
            acknowledgment.acknowledge();
        }
    }
    
    private void sendToDLQ(EmployeeEventDTO event, String topic, 
                          Long offset, Integer partition, String errorMessage) {
        try {
            DlqEvent dlqEvent = DlqEvent.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType().name())
                    .sourceTable("employees")
                    .sourceId(event.getEmployeeId())
                    .eventPayload(objectMapper.writeValueAsString(event))
                    .errorMessage(errorMessage)
                    .stackTrace(getStackTrace())
                    .kafkaOffset(offset)
                    .kafkaPartition(partition)
                    .kafkaTopic(topic)
                    .correlationId(event.getCorrelationId())
                    .build();
            
            dlqRepository.save(dlqEvent);
            dlqCounter.increment();
            
        } catch (Exception e) {
            log.error("CRÍTICO: Erro ao salvar na DLQ: {}", e.getMessage(), e);
            // Fallback: log em arquivo
            logToDlqFile(event, errorMessage);
        }
    }
    }

```

### API REST para DLQ

```

@RestController
@RequestMapping("/api/v1/validation")
public class ValidationReportController {

    // Lista eventos DLQ
    @GetMapping("/dlq")
    public ResponseEntity<List<DlqEventDTO>> getDlqEvents(
            @RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(dlqService.findByStatus(status));
    }
    
    // Reprocessar evento
    @PostMapping("/dlq/{id}/retry")
    public ResponseEntity<Void> retryDlqEvent(@PathVariable Long id) {
        dlqService.retryEvent(id);
        return ResponseEntity.accepted().build();
    }
    
    // Marcar como resolvido
    @PutMapping("/dlq/{id}/resolve")
    public ResponseEntity<Void> resolveDlqEvent(
            @PathVariable Long id,
            @RequestParam String resolvedBy) {
        dlqService.resolveEvent(id, resolvedBy);
        return ResponseEntity.ok().build();
    }
    
    // Descartar evento
    @DeleteMapping("/dlq/{id}")
    public ResponseEntity<Void> discardDlqEvent(@PathVariable Long id) {
        dlqService.discardEvent(id);
        return ResponseEntity.noContent().build();
    }
    }

```

### Queries de Análise

```

-- 1. Eventos pendentes na DLQ
SELECT
id,
event_id,
source_id,
error_message,
created_at
FROM public.dlq_events
WHERE status = 'PENDING'
ORDER BY created_at DESC;

-- 2. Top 10 erros mais comuns
SELECT
error_message,
COUNT(*) as occurrences
FROM public.dlq_events
WHERE status = 'PENDING'
GROUP BY error_message
ORDER BY occurrences DESC
LIMIT 10;

-- 3. Taxa de erro por hora
SELECT
DATE_TRUNC('hour', created_at) as hour,
COUNT(*) as error_count
FROM public.dlq_events
WHERE created_at > CURRENT_DATE - INTERVAL '7 days'
GROUP BY hour
ORDER BY hour;

-- 4. Eventos por tipo
SELECT
event_type,
COUNT(*) as total,
SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending,
SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) as resolved
FROM public.dlq_events
GROUP BY event_type;

-- 5. SLA de resolução (tempo médio até resolver)
SELECT
AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600) as avg_hours_to_resolve
FROM public.dlq_events
WHERE status = 'RESOLVED'
AND resolved_at IS NOT NULL;

```

## Dashboard

### Métricas Expostas

```


# Prometheus

dlq_events_total{status="pending"} 150
dlq_events_total{status="resolved"} 450
dlq_events_total{status="discarded"} 50

# Taxa de erro

rate(dlq_events_total[5m])

# SLA de resolução

dlq_resolution_time_seconds_bucket

```

### Alertas

```


# Alerta se DLQ > 1000 eventos

- alert: DLQTooManyEvents
expr: dlq_events_total{status="pending"} > 1000
for: 5m
annotations:
summary: "DLQ com muitos eventos pendentes"


# Alerta se taxa de erro > 10 eventos/min

- alert: DLQHighErrorRate
expr: rate(dlq_events_total[1m]) > 10
for: 5m
annotations:
summary: "Taxa de erro muito alta"

```

## Política de Purge

```

-- Purge eventos resolvidos > 90 dias
DELETE FROM public.dlq_events
WHERE status = 'RESOLVED'
AND resolved_at < CURRENT_DATE - INTERVAL '90 days';

-- Arquivar antes de purgar
INSERT INTO archive.dlq_events_archive
SELECT * FROM public.dlq_events
WHERE status = 'RESOLVED'
AND resolved_at < CURRENT_DATE - INTERVAL '90 days';

```

## Processo de Reprocessamento

1. **Identificar evento na DLQ**
2. **Analisar erro** (via dashboard ou SQL)
3. **Corrigir problema** (dado ou código)
4. **Opções de reprocessamento:**
   - **A)** API REST `/dlq/{id}/retry`
   - **B)** Republicar no Kafka manualmente
   - **C)** Corrigir no banco e resolver

## Evolução Futura

### Sprint 3
- [ ] Retry automático para erros técnicos (3 tentativas)
- [ ] Classificação de erros (técnico vs validação)
- [ ] Dead Letter Queue secundária (DLQ da DLQ)

### Sprint 4
- [ ] Interface web para visualização e reprocessamento
- [ ] Bulk reprocessing (múltiplos eventos)
- [ ] Workflow de aprovação para reprocessamento

## Links

* [DlqEvent.java](../../consumer-service/src/main/java/com/esocial/consumer/model/entity/DlqEvent.java)
* [DlqEventRepository.java](../../consumer-service/src/main/java/com/esocial/consumer/repository/DlqEventRepository.java)
* [KafkaConsumerService.java](../../consumer-service/src/main/java/com/esocial/consumer/service/KafkaConsumerService.java)
* [ValidationReportController.java](../../consumer-service/src/main/java/com/esocial/consumer/controller/ValidationReportController.java)

## Notas

- Consumer **sempre** commita offset, mesmo com erro (para não bloquear)
- DLQ garante que nenhum evento seja perdido
- Payload completo em JSONB permite reprocessamento exato
- Correlation ID permite rastrear evento end-to-end
- Status PENDING requer ação manual (análise obrigatória)
- Fallback em arquivo se falha ao inserir na DLQ