# Diagrama de Deployment - Infraestrutura

**Versão:** 1.0  
**Data:** 2025-11-22  
**Autor:** Márcio Kuroki Gonçalves  
**Projeto:** Pipeline ETL eSocial

---

## Índice

1. [Visão Geral](#visão-geral)
2. [Ambiente Docker Compose (Dev/Homolog)](#ambiente-docker-compose-devhomolog)
3. [Topologia de Rede](#topologia-de-rede)
4. [Recursos Computacionais](#recursos-computacionais)
5. [Volumes Persistentes](#volumes-persistentes)
6. [Health Checks](#health-checks)
7. [Ambiente de Produção (Kubernetes)](#ambiente-de-produção-kubernetes)
8. [Estratégia de Escala](#estratégia-de-escala)
9. [Segurança e Compliance](#segurança-e-compliance)

---

## Visão Geral

Este documento detalha a **arquitetura de deployment** do Pipeline ETL eSocial em diferentes ambientes:

- **Desenvolvimento:** Docker Compose (local)
- **Homologação:** Docker Compose (servidor dedicado)
- **Produção:** Kubernetes + Helm (futuro)

---

## Ambiente Docker Compose (Dev/Homolog)

### Diagrama PlantUML - Deployment Node

```

@startuml Deployment - Docker Compose
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Deployment.puml

LAYOUT_WITH_LEGEND()

Deployment_Node(docker_host, "Docker Host", "Ubuntu 22.04 LTS") {
Deployment_Node(docker_engine, "Docker Engine", "24.0.7") {

        ' ========================================
        ' NETWORK LAYER
        ' ========================================
        Deployment_Node(network, "esocial-network", "Bridge Network (172.20.0.0/16)") {
            
            ' ========================================
            ' COORDINATION LAYER
            ' ========================================
            Deployment_Node(zk_container, "Container: Zookeeper", "Docker") {
                Container(zookeeper, "Apache Zookeeper", "3.8.0", "Coordenação Kafka\nPort: 2181\nIP: 172.20.0.10")
            }
            
            ' ========================================
            ' KAFKA CLUSTER (3 BROKERS)
            ' ========================================
            Deployment_Node(kafka_cluster, "Kafka Cluster", "3 Containers") {
                
                Deployment_Node(broker1_container, "Container: Kafka Broker 1", "Docker") {
                    Container(broker1, "Kafka Broker 1", "Confluent 7.5.0", "Leader: Partitions 0,1\nPort: 9092\nIP: 172.20.0.11")
                }
                
                Deployment_Node(broker2_container, "Container: Kafka Broker 2", "Docker") {
                    Container(broker2, "Kafka Broker 2", "Confluent 7.5.0", "Leader: Partition 2\nPort: 9093\nIP: 172.20.0.12")
                }
                
                Deployment_Node(broker3_container, "Container: Kafka Broker 3", "Docker") {
                    Container(broker3, "Kafka Broker 3", "Confluent 7.5.0", "Follower: All partitions\nPort: 9094\nIP: 172.20.0.13")
                }
            }
            
            ' ========================================
            ' APPLICATION LAYER
            ' ========================================
            Deployment_Node(producer_container, "Container: Producer Service", "Docker") {
                Container(producer, "Producer Service", "Spring Boot 3.2\nOpenJDK 21", "CDC + Kafka Producer\nPort: 8081\nIP: 172.20.0.20")
            }
            
            Deployment_Node(consumer_container, "Container: Consumer Service", "Docker") {
                Container(consumer, "Consumer Service", "Spring Boot 3.2\nOpenJDK 21", "Validation + Persistence\nPort: 8082\nIP: 172.20.0.21")
            }
            
            ' ========================================
            ' DATABASE LAYER
            ' ========================================
            Deployment_Node(postgres_container, "Container: PostgreSQL", "Docker") {
                ContainerDb(postgres, "PostgreSQL", "15.4-alpine", "3 Schemas:\n- source (origem)\n- public (destino)\n- audit (histórico)\nPort: 5432\nIP: 172.20.0.30")
            }
            
            ' ========================================
            ' OBSERVABILITY LAYER
            ' ========================================
            Deployment_Node(observability_cluster, "Observability Stack", "5 Containers") {
                
                Deployment_Node(prometheus_container, "Container: Prometheus", "Docker") {
                    Container(prometheus, "Prometheus", "2.45.0", "Time-series DB\nPort: 9090\nIP: 172.20.0.40")
                }
                
                Deployment_Node(alertmanager_container, "Container: Alertmanager", "Docker") {
                    Container(alertmanager, "Alertmanager", "0.26.0", "Alert routing\nPort: 9093\nIP: 172.20.0.41")
                }
                
                Deployment_Node(grafana_container, "Container: Grafana", "Docker") {
                    Container(grafana, "Grafana", "10.0.0", "Dashboards\nPort: 3000\nIP: 172.20.0.42")
                }
                
                Deployment_Node(kafka_ui_container, "Container: Kafka UI", "Docker") {
                    Container(kafka_ui, "Kafka UI", "Provectus latest", "Kafka admin UI\nPort: 8090\nIP: 172.20.0.43")
                }
                
                Deployment_Node(pgadmin_container, "Container: PgAdmin", "Docker") {
                    Container(pgadmin, "PgAdmin 4", "8.0", "PostgreSQL admin UI\nPort: 5050\nIP: 172.20.0.44")
                }
            }
        }
        
        ' ========================================
        ' VOLUMES (PERSISTENT STORAGE)
        ' ========================================
        Deployment_Node(volumes, "Docker Volumes", "Local Storage") {
            ContainerDb(zk_data, "zookeeper-data", "Volume", "2 GB")
            ContainerDb(kafka1_data, "kafka-broker-1-data", "Volume", "10 GB")
            ContainerDb(kafka2_data, "kafka-broker-2-data", "Volume", "10 GB")
            ContainerDb(kafka3_data, "kafka-broker-3-data", "Volume", "10 GB")
            ContainerDb(postgres_data, "postgres-data", "Volume", "20 GB")
            ContainerDb(prometheus_data, "prometheus-data", "Volume", "5 GB")
            ContainerDb(grafana_data, "grafana-data", "Volume", "2 GB")
        }
    }
    }

' ========================================
' EXTERNAL SYSTEMS
' ========================================
System_Ext(external_user, "Usuário/Operador", "Acessa dashboards\ne APIs REST")

System_Ext(external_esocial, "Portal eSocial", "Governo Federal\n(Futuro)")

' ========================================
' RELATIONSHIPS
' ========================================

' Coordination
Rel(broker1, zookeeper, "Cluster coordination", "Zookeeper Protocol")
Rel(broker2, zookeeper, "Cluster coordination", "Zookeeper Protocol")
Rel(broker3, zookeeper, "Cluster coordination", "Zookeeper Protocol")

' Kafka replication
Rel(broker1, broker2, "Replication (RF=3)", "Kafka Protocol")
Rel(broker1, broker3, "Replication (RF=3)", "Kafka Protocol")
Rel(broker2, broker3, "Replication (RF=3)", "Kafka Protocol")

' Application → Kafka
Rel(producer, broker1, "Publish events", "Kafka Protocol :9092")
Rel(producer, broker2, "Publish events", "Kafka Protocol :9093")
Rel(producer, broker3, "Publish events", "Kafka Protocol :9094")

Rel(consumer, broker1, "Consume events", "Kafka Protocol :9092")
Rel(consumer, broker2, "Consume events", "Kafka Protocol :9093")
Rel(consumer, broker3, "Consume events", "Kafka Protocol :9094")

' Application → Database
Rel(producer, postgres, "CDC Polling\n(SELECT)", "JDBC :5432")
Rel(consumer, postgres, "Persistence\n(INSERT/UPDATE)", "JDBC :5432")

' Observability
Rel(prometheus, producer, "Scrape /actuator/prometheus", "HTTP :8081")
Rel(prometheus, consumer, "Scrape /actuator/prometheus", "HTTP :8082")
Rel(prometheus, alertmanager, "Send alerts", "HTTP :9093")
Rel(grafana, prometheus, "Query metrics", "PromQL :9090")

' External access
Rel(external_user, grafana, "View dashboards", "HTTPS :3000")
Rel(external_user, kafka_ui, "Manage Kafka", "HTTP :8090")
Rel(external_user, pgadmin, "Manage PostgreSQL", "HTTP :5050")
Rel(external_user, consumer, "API REST\n(/api/v1/validation/*)", "HTTP :8082")

' Future
Rel_Back(consumer, external_esocial, "Envio eventos\n(Futuro)", "HTTPS + Certificado Digital")

' Volume mounts
Rel(zookeeper, zk_data, "Mount", "/data")
Rel(broker1, kafka1_data, "Mount", "/var/lib/kafka/data")
Rel(broker2, kafka2_data, "Mount", "/var/lib/kafka/data")
Rel(broker3, kafka3_data, "Mount", "/var/lib/kafka/data")
Rel(postgres, postgres_data, "Mount", "/var/lib/postgresql/data")
Rel(prometheus, prometheus_data, "Mount", "/prometheus")
Rel(grafana, grafana_data, "Mount", "/var/lib/grafana")

@enduml

```

---

## Topologia de Rede

### Subnet: esocial-network (172.20.0.0/16)

| Container | IP Estático | Porta(s) Externa | Porta(s) Interna |
|-----------|-------------|------------------|------------------|
| **Coordination** | | | |
| zookeeper | 172.20.0.10 | 2181 | 2181, 2888, 3888 |
| **Kafka Cluster** | | | |
| kafka-broker-1 | 172.20.0.11 | 9092 | 9092, 29092 |
| kafka-broker-2 | 172.20.0.12 | 9093 | 9093, 29093 |
| kafka-broker-3 | 172.20.0.13 | 9094 | 9094, 29094 |
| **Application** | | | |
| producer-service | 172.20.0.20 | 8081 | 8081, 8080 |
| consumer-service | 172.20.0.21 | 8082 | 8082, 8080 |
| **Database** | | | |
| postgres-db | 172.20.0.30 | 5432 | 5432 |
| **Observability** | | | |
| prometheus | 172.20.0.40 | 9090 | 9090 |
| alertmanager | 172.20.0.41 | 9093 | 9093, 9094 |
| grafana | 172.20.0.42 | 3000 | 3000 |
| kafka-ui | 172.20.0.43 | 8090 | 8080 |
| pgadmin | 172.20.0.44 | 5050 | 80 |

### Comunicação Entre Containers

```

┌────────────────────────────────────────────────────────┐
│                  ESOCIAL-NETWORK                       │
│                  (172.20.0.0/16)                       │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌──────────┐         ┌─────────────────────────┐      │
│  │Zookeeper │◄────────│  Kafka Cluster          │      │
│  │:2181     │         │  Broker1:9092           │      │
│  └──────────┘         │  Broker2:9093           │      │
│       ▲               │  Broker3:9094           │      │
│       │               └─────────▲────┬──────────┘      │
│       │                         │    │                 │
│       │                         │    │                 │
│  ┌────┴─────┐          ┌────────┴────▼─────────┐       │
│  │Producer  │◄─────────┤  PostgreSQL           │       │
│  │:8081     │  CDC     │  :5432                │       │
│  └──────────┘  Polling │  (3 schemas)          │       │
│       │                └───────────▲───────────┘       │
│       │                            │                   │
│       │                            │ Persist           │
│       │  Kafka                     │                   │
│       ▼  Protocol         ┌────────┴─────────┐         │
│  ┌─────────────┐          │  Consumer        │         │
│  │Kafka Cluster│◄─────────│  :8082           │         │
│  └──────┬──────┘          └──────────────────┘         │
│         │                                              │
│         │                                              │
│    ┌────▼────────┐     ┌──────────────┐                │
│    │Prometheus   │────►│  Grafana     │                │
│    │:9090        │     │  :3000       │                │
│    └─────────────┘     └──────────────┘                │
│                                                        │
└────────────────────────────────────────────────────────┘

```

---

## Recursos Computacionais

### Alocação de Recursos (Desenvolvimento)

| Container | CPU Cores | RAM | Disco | JVM Heap | Prioridade |
|-----------|-----------|-----|-------|----------|------------|
| **Kafka Cluster** | | | | | |
| kafka-broker-1 | 1.0 | 2 GB | 10 GB | N/A | Alta |
| kafka-broker-2 | 1.0 | 2 GB | 10 GB | N/A | Alta |
| kafka-broker-3 | 1.0 | 2 GB | 10 GB | N/A | Alta |
| **Application** | | | | | |
| producer-service | 0.5 | 1 GB | 1 GB | 512 MB | Crítica |
| consumer-service | 1.0 | 2 GB | 1 GB | 1 GB | Crítica |
| **Infrastructure** | | | | | |
| zookeeper | 0.5 | 512 MB | 2 GB | N/A | Alta |
| postgres-db | 2.0 | 4 GB | 20 GB | N/A | Crítica |
| **Observability** | | | | | |
| prometheus | 0.5 | 1 GB | 5 GB | N/A | Média |
| alertmanager | 0.25 | 512 MB | 1 GB | N/A | Média |
| grafana | 0.25 | 512 MB | 2 GB | N/A | Baixa |
| kafka-ui | 0.25 | 512 MB | 1 GB | N/A | Baixa |
| pgadmin | 0.25 | 512 MB | 1 GB | N/A | Baixa |
| **TOTAL** | **8.25** | **16 GB** | **65 GB** | **1.5 GB** | - |

### Requisitos Mínimos do Host

```

Sistema Operacional: Ubuntu 22.04 LTS ou superior
CPU: 8 cores (mínimo) / 16 cores (recomendado)
RAM: 16 GB (mínimo) / 32 GB (recomendado)
Disco: 100 GB SSD (mínimo) / 500 GB SSD (recomendado)
Docker: 24.0+
Docker Compose: 2.20+

```

---

## Volumes Persistentes

### Estratégia de Volumes

| Volume | Tipo | Tamanho | Backup | Descrição |
|--------|------|---------|--------|-----------|
| **zookeeper-data** | Named | 2 GB | Não | Metadados Zookeeper |
| **kafka-broker-1-data** | Named | 10 GB | Sim | Logs Kafka (partições 0,1) |
| **kafka-broker-2-data** | Named | 10 GB | Sim | Logs Kafka (partição 2) |
| **kafka-broker-3-data** | Named | 10 GB | Sim | Logs Kafka (réplicas) |
| **postgres-data** | Named | 20 GB | **Sim (Crítico)** | Dados PostgreSQL |
| **prometheus-data** | Named | 5 GB | Não | Métricas (7 dias retenção) |
| **grafana-data** | Named | 2 GB | Sim | Dashboards e configs |

### Política de Retenção

```


# Kafka Topics

retention.ms: 604800000  \# 7 dias
retention.bytes: 10737418240  \# 10 GB por partição

# Prometheus

retention.time: 7d  \# 7 dias
retention.size: 5GB

# PostgreSQL

# Sem retenção automática (dados permanentes)

# Backup diário via pg_dump

```

### Comandos de Backup

```

\#!/bin/bash

# scripts/backup-volumes.sh

# Backup PostgreSQL

docker exec esocial-postgres-db pg_dump -U esocial_user esocial \
> backups/postgres_\$(date +%Y%m%d_%H%M%S).sql

# Backup Grafana dashboards

docker cp esocial-grafana:/var/lib/grafana/grafana.db \
backups/grafana_\$(date +%Y%m%d_%H%M%S).db

# Backup Kafka data (snapshot)

docker exec esocial-kafka-broker-1 kafka-log-dirs \
--describe --bootstrap-server localhost:9092 \
> backups/kafka_log_dirs_\$(date +%Y%m%d_%H%M%S).txt

```

---

## Health Checks

### Configuração de Health Checks

#### Producer Service

```

healthcheck:
test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
interval: 30s
timeout: 10s
retries: 3
start_period: 60s

```

#### Consumer Service

```

healthcheck:
test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
interval: 30s
timeout: 10s
retries: 3
start_period: 60s

```

#### PostgreSQL

```

healthcheck:
test: ["CMD-SHELL", "pg_isready -U esocial_user -d esocial"]
interval: 10s
timeout: 5s
retries: 5
start_period: 30s

```

#### Kafka Brokers

```

healthcheck:
test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
interval: 10s
timeout: 10s
retries: 5
start_period: 60s

```

### Monitoramento de Health

```


# Verificar status de todos os containers

docker-compose ps

# Health check manual

curl http://localhost:8081/actuator/health | jq
curl http://localhost:8082/actuator/health | jq

# Logs de health checks

docker inspect --format='{{json .State.Health}}' esocial-producer-service | jq

```

---

## Ambiente de Produção (Kubernetes)

### Arquitetura Kubernetes (Futuro)

```

@startuml Kubernetes Production
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Deployment.puml

Deployment_Node(k8s_cluster, "Kubernetes Cluster", "EKS/GKE/AKS") {

    Deployment_Node(namespace_esocial, "Namespace: esocial-prod", "Kubernetes Namespace") {
        
        ' Producer
        Deployment_Node(producer_deployment, "Deployment: producer", "3 replicas") {
            Container(producer_pod1, "Pod: producer-1", "Spring Boot", "Auto-scaling: 3-10")
            Container(producer_pod2, "Pod: producer-2", "Spring Boot", "")
            Container(producer_pod3, "Pod: producer-3", "Spring Boot", "")
        }
        
        ' Consumer
        Deployment_Node(consumer_deployment, "Deployment: consumer", "5 replicas") {
            Container(consumer_pod1, "Pod: consumer-1", "Spring Boot", "Auto-scaling: 5-20")
            Container(consumer_pod2, "Pod: consumer-2", "Spring Boot", "")
            Container(consumer_pod3, "Pod: consumer-3", "Spring Boot", "")
        }
        
        ' Services
        Container(producer_svc, "Service: producer-svc", "ClusterIP", "Port: 8081")
        Container(consumer_svc, "Service: consumer-svc", "LoadBalancer", "Port: 8082")
        
        ' Ingress
        Container(ingress, "Ingress: esocial-ingress", "Nginx", "SSL/TLS Termination")
    }
    
    ' External Services
    Deployment_Node(kafka_external, "External: Kafka", "MSK/Confluent Cloud") {
        Container(kafka_managed, "Managed Kafka", "Confluent 7.5", "3 brokers + auto-scaling")
    }
    
    Deployment_Node(db_external, "External: Database", "RDS PostgreSQL") {
        ContainerDb(postgres_managed, "RDS PostgreSQL", "15.4", "Multi-AZ + Read Replicas")
    }
    }

System_Ext(users, "Usuários", "APIs REST")
System_Ext(esocial_gov, "Portal eSocial", "Governo Federal")

Rel(users, ingress, "HTTPS", "443")
Rel(ingress, consumer_svc, "HTTP", "8082")
Rel(consumer_svc, consumer_pod1, "Load balance", "8082")

Rel(producer_pod1, kafka_managed, "Produce", "TLS")
Rel(consumer_pod1, kafka_managed, "Consume", "TLS")

Rel(producer_pod1, postgres_managed, "CDC", "TLS")
Rel(consumer_pod1, postgres_managed, "Persist", "TLS")

Rel(consumer_pod1, esocial_gov, "Envio eventos", "HTTPS + Cert Digital")

@enduml

```

### Manifests Kubernetes (Exemplo)

#### Deployment - Consumer Service

```

apiVersion: apps/v1
kind: Deployment
metadata:
name: consumer-service
namespace: esocial-prod
labels:
app: consumer-service
version: v1.0.0
spec:
replicas: 5
selector:
matchLabels:
app: consumer-service
template:
metadata:
labels:
app: consumer-service
version: v1.0.0
spec:
containers:
- name: consumer
image: marciokuroki/esocial-consumer:1.0.0
ports:
- containerPort: 8082
name: http
- containerPort: 8080
name: actuator
env:
- name: SPRING_PROFILES_ACTIVE
value: "production"
- name: KAFKA_BOOTSTRAP_SERVERS
valueFrom:
secretKeyRef:
name: kafka-config
key: bootstrap-servers
- name: DATABASE_URL
valueFrom:
secretKeyRef:
name: postgres-config
key: jdbc-url
resources:
requests:
cpu: 500m
memory: 1Gi
limits:
cpu: 2000m
memory: 4Gi
livenessProbe:
httpGet:
path: /actuator/health/liveness
port: 8080
initialDelaySeconds: 60
periodSeconds: 10
readinessProbe:
httpGet:
path: /actuator/health/readiness
port: 8080
initialDelaySeconds: 30
periodSeconds: 5
***
apiVersion: v1
kind: Service
metadata:
name: consumer-service
namespace: esocial-prod
spec:
type: LoadBalancer
selector:
app: consumer-service
ports:

- name: http
port: 8082
targetPort: 8082

```

#### HorizontalPodAutoscaler

```

apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
name: consumer-hpa
namespace: esocial-prod
spec:
scaleTargetRef:
apiVersion: apps/v1
kind: Deployment
name: consumer-service
minReplicas: 5
maxReplicas: 20
metrics:

- type: Resource
resource:
name: cpu
target:
type: Utilization
averageUtilization: 70
- type: Resource
resource:
name: memory
target:
type: Utilization
averageUtilization: 80

```

---

## Estratégia de Escala

### Fases de Crescimento

#### Fase 1: MVP (Atual - Docker Compose)
```

Throughput: 1.000 eventos/segundo
Infraestrutura:
├── 1 host (16 GB RAM, 8 cores)
├── 3 Kafka brokers (standalone)
├── 1 Producer instance
├── 1 Consumer instance
└── 1 PostgreSQL instance

```

#### Fase 2: Produção (Kubernetes)
```

Throughput: 10.000 eventos/segundo
Infraestrutura:
├── 3 worker nodes (32 GB RAM, 16 cores cada)
├── Kafka MSK/Confluent Cloud (5 brokers)
├── 3 Producer pods (auto-scaling 3-10)
├── 5 Consumer pods (auto-scaling 5-20)
└── RDS PostgreSQL Multi-AZ (db.r5.2xlarge)

```

#### Fase 3: Enterprise (Alta Demanda)
```

Throughput: 100.000 eventos/segundo
Infraestrutura:
├── 10 worker nodes (64 GB RAM, 32 cores cada)
├── Kafka + Schema Registry (10+ brokers)
├── Producer: Auto-scaling 10-50 pods
├── Consumer: Auto-scaling 20-100 pods
├── RDS PostgreSQL Multi-AZ + 3 Read Replicas
└── Service Mesh (Istio) para observabilidade

```

---

## Segurança e Compliance

### Segurança em Produção

| Componente | Medida de Segurança | Status |
|------------|---------------------|--------|
| **Kafka** | TLS 1.3 encryption | Planejado |
| **Kafka** | SASL/SCRAM authentication | Planejado |
| **PostgreSQL** | TLS connections | Planejado |
| **PostgreSQL** | Role-based access (RLS) | Planejado |
| **APIs** | JWT authentication | Planejado |
| **Secrets** | Kubernetes Secrets / Vault | Planejado |
| **Network** | Network Policies (Calico) | Planejado |
| **eSocial** | Certificado Digital A1/A3 | Planejado |

### Compliance LGPD

- ✅ **Audit Trail:** Histórico completo em `audit.employees_history`
- ✅ **Soft Delete:** Dados preservados para auditoria
- ✅ **Encryption at Rest:** Volumes criptografados (produção)
- ✅ **Encryption in Transit:** TLS 1.3 (produção)
- ✅ **Right to be Forgotten:** Procedimento de anonimização

---

## Monitoramento de Infraestrutura

### Métricas de Sistema

```


# Prometheus - Node Exporter

node_cpu_seconds_total
node_memory_MemAvailable_bytes
node_disk_io_time_seconds_total
node_network_receive_bytes_total

# Prometheus - Kafka Exporter

kafka_consumergroup_lag
kafka_topic_partition_current_offset
kafka_topic_partition_leader

# Prometheus - PostgreSQL Exporter

pg_stat_database_tup_inserted
pg_stat_database_tup_updated
pg_locks_count

```

### Alertas de Infraestrutura

```

groups:

- name: infrastructure
rules:
    - alert: HighCPUUsage
expr: 100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
for: 10m
labels:
severity: warning
    - alert: HighMemoryUsage
expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 90
for: 5m
labels:
severity: critical
    - alert: DiskSpaceLow
expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) * 100 < 10
for: 5m
labels:
severity: critical

```

---

## Referências

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [Kafka on Kubernetes](https://strimzi.io/)
- [PostgreSQL High Availability](https://www.postgresql.org/docs/15/high-availability.html)
- [Prometheus Monitoring](https://prometheus.io/docs/introduction/overview/)

---

**Última atualização:** 2025-11-22  
**Autor:** Márcio Kuroki Gonçalves