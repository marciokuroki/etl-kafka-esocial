# Producer Service

Serviço responsável por capturar mudanças de dados (CDC) no sistema de origem e publicar eventos no Kafka.

## Funcionalidades

- Change Data Capture (CDC) simulado com polling
- Publicação de eventos no Kafka com Correlation ID propagado nos headers Kafka
- Métricas Prometheus (eventos publicados, falhas, latência, tamanho de payload)
- Health checks
- Logs estruturados em JSON com Logback e Logstash Encoder

## Uso do Correlation ID

- Cada evento possui um campo `correlationId` do tipo UUID incluído no DTO `EmployeeEventDTO`.
- O `correlationId` é enviado no header Kafka `"X-Correlation-Id"`, garantindo rastreabilidade em todo pipeline.
- Logs estruturados incluem esse ID no contexto MDC para facilitar análise e troubleshooting.

## Configuração

Ver `application.yml` para configurações de:
- Banco de dados (PostgreSQL)
- Kafka brokers
- Intervalo de polling CDC
- Tamanho de batch

Logback configurado em `src/main/resources/logback-spring.xml` para logs JSON.

## Executar Localmente
```
mvn spring-boot:run
```
## Compilar
```
mvn clean package
```
## Docker
```
docker build -t producer-service:latest .
docker run -p 8081:8081 producer-service:latest
```

## Endpoints

- Health: http://localhost:8081/actuator/health
- Métricas: http://localhost:8081/actuator/prometheus
- Info: http://localhost:8081/actuator/info

## Exemplo de criação e envio de evento com correlation ID
```
UUID correlationId = UUID.randomUUID();
EmployeeEventDTO event = EmployeeEventDTO.builder()
.eventId(UUID.randomUUID().toString())
.correlationId(correlationId)
// outros campos obrigatórios
.build();
producerService.publishEmployeeEvent(event);
```