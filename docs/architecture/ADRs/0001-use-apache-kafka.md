# 0001. Uso do Apache Kafka como Message Broker

**Status:** Aceito  
**Data:** 2025-11-01  
**Decisores:** Márcio Kuroki Gonçalves  
**Tags:** infraestrutura, message-broker, kafka

## Contexto e Problema

O Pipeline ETL eSocial precisa processar eventos de mudança de dados de forma assíncrona e escalável. O sistema deve:

- Desacoplar o produtor (CDC) do consumidor (validação/persistência)
- Garantir que nenhum evento seja perdido
- Permitir reprocessamento de eventos em caso de falha
- Suportar escalabilidade horizontal
- Fornecer alta disponibilidade

**Problema:** Qual message broker utilizar para transporte de eventos?

## Fatores de Decisão

* Garantia de entrega (at-least-once)
* Throughput (eventos/segundo)
* Latência
* Facilidade de operação
* Maturidade da tecnologia
* Suporte a replicação
* Custo de infraestrutura
* Curva de aprendizado da equipe

## Opções Consideradas

* Apache Kafka
* RabbitMQ
* AWS SQS
* Redis Streams
* Apache Pulsar

## Decisão

**Escolhido:** Apache Kafka

**Justificativa:** Kafka oferece o melhor equilíbrio entre throughput, garantias de entrega, e capacidade de replay de mensagens, que são fundamentais para um sistema ETL confiável.

## Consequências

### Positivas

* ✅ **Alta throughput**: Capaz de processar centenas de milhares de eventos por segundo
* ✅ **Garantia de entrega**: Configuração de acks=all garante que mensagens sejam replicadas
* ✅ **Retenção de mensagens**: Permite replay de eventos para reprocessamento
* ✅ **Particionamento**: Possibilita paralelismo e escalabilidade horizontal
* ✅ **Replicação nativa**: Alta disponibilidade com múltiplos brokers
* ✅ **Ecossistema maduro**: Ferramentas de monitoramento e gerenciamento disponíveis
* ✅ **Integração Spring**: Spring Kafka facilita implementação

### Negativas

* ❌ **Complexidade operacional**: Requer conhecimento específico para operar (Zookeeper, brokers, partições)
* ❌ **Overhead de recursos**: Consome mais memória que alternativas mais leves
* ❌ **Curva de aprendizado**: Conceitos de partições, offsets, consumer groups requerem estudo
* ❌ **Não é ideal para filas clássicas**: Não possui features como priority queues

### Riscos

* **Risco de perda de mensagens se mal configurado**: Mitigado com acks=all e min.insync.replicas=2
* **Complexidade em troubleshooting**: Mitigado com Kafka UI e ferramentas de monitoramento
* **Single point of failure (Zookeeper)**: Mitigado com cluster ZK de 3 nós

## Alternativas

### RabbitMQ

**Descrição:** Message broker baseado em AMQP, focado em filas tradicionais.

**Prós:**
- Mais simples de operar
- Menor consumo de recursos
- Suporte a múltiplos protocolos
- Boa interface de administração

**Contras:**
- Menor throughput que Kafka
- Não possui retenção de mensagens nativa
- Reprocessamento mais complexo
- Menos adequado para event streaming

**Por que foi rejeitada:** Não oferece capacidade de replay de eventos, que é fundamental para o cenário de ETL onde pode ser necessário reprocessar dados históricos.

### AWS SQS

**Descrição:** Serviço gerenciado de filas da AWS.

**Prós:**
- Totalmente gerenciado (sem operação)
- Escalabilidade automática
- Integração com AWS
- Baixo custo inicial

**Contras:**
- Vendor lock-in
- Latência mais alta
- Limites de mensagem (256KB)
- Não possui conceito de partições
- Custo por mensagem

**Por que foi rejeitada:** Projeto precisa rodar on-premises e ter controle total da infraestrutura. Além disso, limitações de tamanho de mensagem podem ser problemáticas.

### Redis Streams

**Descrição:** Feature de streaming do Redis.

**Prós:**
- Muito baixa latência
- Simples de configurar
- Já familiar para a equipe
- Leve

**Contras:**
- Menos maduro que Kafka para event streaming
- Ferramentas de operação limitadas
- Replicação não tão robusta
- Não é seu caso de uso primário

**Por que foi rejeitada:** Não é tão maduro quanto Kafka para cenários de event streaming em produção. Falta de ferramentas empresariais de monitoramento.

## Validação

A decisão será validada através de:

1. **Teste de carga:** Processar 10.000 eventos/segundo por 1 hora
   - Métrica: 100% de entregas com latência P95 < 100ms
   
2. **Teste de falha:** Derrubar 1 broker durante processamento
   - Métrica: Zero perda de mensagens, recuperação automática

3. **Teste de replay:** Reprocessar 100.000 eventos históricos
   - Métrica: Todas as mensagens reprocessadas com sucesso

4. **Monitoramento em produção (primeiros 30 dias):**
   - Consumer lag < 1000 mensagens
   - Availability > 99.9%
   - Zero perda de dados

## Implementação

- **Versão:** Confluent Platform 7.5.0
- **Configuração:** 3 brokers, RF=3, min.insync.replicas=2
- **Tópicos:** 
  - employee-create (3 partitions)
  - employee-update (3 partitions)
  - employee-delete (3 partitions)
  - esocial-dlq (3 partitions)

## Links

* [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
* [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/html/)
* [Card 1.2 - Provisionamento do Cluster Kafka](../../README.md#card-12)

## Notas

- Considerar migração para KRaft (sem Zookeeper) em versão futura
- Avaliar Kafka Connect para CDC real em Sprint 3
- Monitorar custos de storage para retenção de 7 dias
