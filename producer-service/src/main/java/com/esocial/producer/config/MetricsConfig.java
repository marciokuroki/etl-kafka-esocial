package com.esocial.producer.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MetricsConfig {

    /**
     * Timer para medir latência do CDC (polling)
     */
    @Bean
    public Timer cdcPollingTimer(MeterRegistry registry) {
        return Timer.builder("cdc.polling.duration")
                .description("Tempo de execução do polling CDC")
                .tag("service", "producer")
                .tag("operation", "polling")
                .register(registry);
    }

    /**
     * Timer para medir latência de publicação no Kafka
     */
    @Bean
    public Timer kafkaPublishTimer(MeterRegistry registry) {
        return Timer.builder("kafka.publish.duration")
                .description("Tempo de publicação de evento no Kafka")
                .tag("service", "producer")
                .register(registry);
    }
}
