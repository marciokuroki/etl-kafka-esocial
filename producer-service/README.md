# Producer Service

Serviço responsável por capturar mudanças de dados (CDC) no sistema de origem e publicar eventos no Kafka.

## Funcionalidades

- Change Data Capture (CDC) simulado com polling
- Publicação de eventos no Kafka
- Métricas Prometheus
- Health checks
- Logs estruturados

## Configuração

Ver `application.yml` para configurações de:
- Banco de dados (PostgreSQL)
- Kafka brokers
- Intervalo de polling CDC
- Tamanho de batch

## Executar Localmente

```mvn spring-boot:run```

## Compilar

```mvn clean package```

## Docker

```
docker build -t producer-service:latest .
docker run -p 8081:8081 producer-service:latest
```

## Endpoints

- Health: http://localhost:8081/actuator/health
- Métricas: http://localhost:8081/actuator/prometheus
- Info: http://localhost:8081/actuator/info

