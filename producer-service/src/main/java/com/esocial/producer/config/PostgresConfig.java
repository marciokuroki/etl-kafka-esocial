package com.esocial.producer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.esocial.producer.repository")
@EnableTransactionManagement
public class PostgresConfig {
    // Configurações adicionais podem ser adicionadas aqui
}
