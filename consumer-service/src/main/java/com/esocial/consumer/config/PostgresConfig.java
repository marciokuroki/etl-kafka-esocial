package com.esocial.consumer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.esocial.consumer.repository")
@EnableTransactionManagement
public class PostgresConfig {
    // Configurações adicionais podem ser adicionadas aqui
}
