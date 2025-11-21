# Configuração do Grafana

Este documento guia sobre a configuração do Grafana no projeto ETL Kafka eSocial.

## Provisionamento automático

- Dashboards JSON são carregados automaticamente via provisioning YAML.
- Datasource Prometheus também provisionado automaticamente.
- Arquivos chave:
  - `config/grafana/provisioning/dashboard_provisioning.yaml`
  - `config/grafana/provisioning/datasources/prometheus.yml`
  - Dashboards JSON em `config/grafana/dashboards/`

## Docker Compose

Volumens montados para provisionamento:
```
./config/grafana/provisioning:/etc/grafana/provisioning
./config/grafana/dashboards:/var/lib/grafana/dashboards
```

## Acesso

- URL: http://localhost:3000
- Usuário padrão: admin
- Senha configurada: `GF_SECURITY_ADMIN_PASSWORD` no docker-compose
