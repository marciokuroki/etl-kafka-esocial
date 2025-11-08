## Serviços Desenvolvidos

### Producer Service ✅

**Status**: Funcionando
**Porta**: 8081
**Função**: Captura mudanças no banco de origem e publica no Kafka

**Endpoints**:
- Health: http://localhost:8081/actuator/health
- Métricas: http://localhost:8081/actuator/prometheus

**Logs**:
```docker-compose logs -f producer-service```

**Tópicos Kafka**:
- `employee-create` - Novos colaboradores
- `employee-update` - Colaboradores atualizados
- `employee-delete` - Colaboradores desligados

### Consumer Service ⏳

**Status**: Em desenvolvimento
**Porta**: 8082 (quando implementado)

## Comandos Úteis

### Build dos Serviços

#### Compilar todos os serviços
```./scripts/build-services.sh```

#### Buildar imagens Docker
```docker-compose build```

#### Subir todos os serviços
```docker-compose up -d```

### Validação

#### Ver status de todos os containers
```docker-compose ps```

#### Health check do Producer
```curl http://localhost:8081/actuator/health```

#### Ver eventos no Kafka UI
Acesse: http://localhost:8090

### Desenvolvimento

#### Rodar Producer localmente (para desenvolvimento)
```
cd producer-service
mvn spring-boot:run
```
#### Ver logs em tempo real
```docker-compose logs -f producer-service```
