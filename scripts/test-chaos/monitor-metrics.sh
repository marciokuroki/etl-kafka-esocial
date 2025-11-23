#!/bin/bash

# Monitorar métricas continuamente

LOG_FILE="logs/metrics-\$(date +%Y%m%d-%H%M%S).csv"

echo "timestamp,throughput,latency_p95,consumer_lag,cpu_producer,cpu_consumer" > \$LOG_FILE

while true; do
    TIMESTAMP=\$(date +%s)
    
    THROUGHPUT=\$(curl -s 'http://localhost:9090/api/v1/query?query=rate(events_published_total[1m])*60' | jq -r '.data.result.value[^1]')
    
    LATENCY=\$(curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(validation_duration_seconds_bucket[1m]))*1000' | jq -r '.data.result.value[^1]')
    
    LAG=\$(curl -s 'http://localhost:9090/api/v1/query?query=kafka_consumergroup_lag' | jq -r '.data.result.value[^1]')
    
    CPU_PROD=\$(docker stats esocial-producer-service --no-stream --format "{{.CPUPerc}}" | tr -d '%')
    
    CPU_CONS=\$(docker stats esocial-consumer-service --no-stream --format "{{.CPUPerc}}" | tr -d '%')
    
    echo "$TIMESTAMP,$THROUGHPUT,$LATENCY,$LAG,$CPU_PROD,$CPU_CONS" >> \$LOG_FILE
    
    sleep 10
done