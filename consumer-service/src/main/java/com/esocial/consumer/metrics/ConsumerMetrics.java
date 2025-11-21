package com.esocial.consumer.metrics;

import com.esocial.consumer.repository.DlqEventRepository;
import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumerMetrics {

    private final MeterRegistry registry;
    private final DlqEventRepository dlqRepository;
    
    private Timer validationTimer;
    private Timer persistenceTimer;
    private DistributionSummary payloadSizeDistribution;

    @PostConstruct
    public void configureMetrics() {
        log.info("Configurando métricas customizadas do Consumer Service");
        
        // Timer: Latência de validação
        validationTimer = Timer.builder("validation.duration")
                .description("Tempo de execução das validações")
                .tag("service", "consumer")
                .register(registry);
        
        // Timer: Latência de persistência
        persistenceTimer = Timer.builder("persistence.duration")
                .description("Tempo de persistência no banco de dados")
                .tag("service", "consumer")
                .register(registry);
        
        // Histogram: Tamanho de payloads
        payloadSizeDistribution = DistributionSummary.builder("events.payload.size")
                .description("Tamanho dos payloads consumidos em bytes")
                .tag("service", "consumer")
                .baseUnit("bytes")
                .register(registry);
        
        // Gauge: Eventos pendentes na DLQ
        Gauge.builder("dlq.events.pending", dlqRepository,
                repo -> {
                    try {
                        return repo.countByStatus("PENDING");
                    } catch (Exception e) {
                        log.error("Erro ao coletar métrica de DLQ: {}", e.getMessage());
                        return 0;
                    }
                })
                .description("Número de eventos pendentes na Dead Letter Queue")
                .tag("service", "consumer")
                .tag("severity", "high")
                .register(registry);
        
        log.info("Métricas configuradas com sucesso");
    }
    
    // Métodos auxiliares para instrumentação
    
    public Timer.Sample startValidation() {
        return Timer.start(registry);
    }
    
    public void stopValidation(Timer.Sample sample) {
        sample.stop(validationTimer);
    }
    
    public Timer.Sample startPersistence() {
        return Timer.start(registry);
    }
    
    public void stopPersistence(Timer.Sample sample) {
        sample.stop(persistenceTimer);
    }
    
    public void recordPayloadSize(int size) {
        payloadSizeDistribution.record(size);
    }
    
    public void incrementConsumedEvents(String eventType, String topic) {
        Counter.builder("events.consumed.total")
                .description("Total de eventos consumidos do Kafka")
                .tag("service", "consumer")
                .tag("event_type", eventType)
                .tag("topic", topic)
                .register(registry)
                .increment();
    }
    
    public void incrementValidationSuccess(String eventType) {
        Counter.builder("validation.success.total")
                .description("Total de validações bem-sucedidas")
                .tag("service", "consumer")
                .tag("event_type", eventType)
                .register(registry)
                .increment();
    }
    
    public void incrementValidationFailure(String eventType, String severity) {
        Counter.builder("validation.failure.total")
                .description("Total de validações que falharam")
                .tag("service", "consumer")
                .tag("event_type", eventType)
                .tag("severity", severity)
                .register(registry)
                .increment();
    }
}
