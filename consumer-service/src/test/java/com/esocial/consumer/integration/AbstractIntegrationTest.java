package com.esocial.consumer.integration;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.esocial.consumer.model.dto.EmployeeEventDTO;

/**
 * Classe base para testes de integração End-to-End
 * 
 * Responsabilidades:
 * - Inicializar containers Testcontainers (Kafka, PostgreSQL origem/destino)
 * - Configurar properties dinâmicas do Spring Boot
 * - Gerenciar ciclo de vida completo dos containers
 * - Logging de containers para debugging
 * 
 * @author Márcio Kuroki Gonçalves
 * @version 1.0
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    // ============================================
    // CONTAINERS TESTCONTAINERS
    // ============================================

    /**
     * Container PostgreSQL - Banco de Origem (simula sistema legado)
     * Usado pelo Producer Service para CDC
     */
    @Container
    static PostgreSQLContainer<?> postgresSource = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("esocial_source")
            .withUsername("esocial_user")
            .withPassword("PostgresPassword123!")
            .withInitScript("test-data/init-source-schema.sql")
            .withReuse(true);  // Reutilizar container entre testes (performance)

    /**
     * Container PostgreSQL - Banco de Destino
     * Usado pelo Consumer Service para persistência
     */
    @Container
    static PostgreSQLContainer<?> postgresTarget = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("esocial_target")
            .withUsername("esocial_user")
            .withPassword("PostgresPassword123!")
            .withInitScript("test-data/init-target-schema.sql")
            .withReuse(true);

    /**
     * Container Kafka - Message Broker
     * Usado para comunicação assíncrona entre Producer e Consumer
     */
    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withReuse(true);

    // ============================================
    // LIFECYCLE METHODS
    // ============================================

    /**
     * Setup executado UMA VEZ antes de todos os testes da classe
     * Inicializa containers e configura logging
     */
    @BeforeAll
    static void setUp() {
        log.info("========================================");
        log.info("Iniciando containers Testcontainers...");
        log.info("========================================");

        // Inicializar containers (em paralelo se possível)
        long startTime = System.currentTimeMillis();

        try {
            // Kafka
            if (!kafka.isRunning()) {
                kafka.start();
                // Configurar logging do container Kafka
                kafka.followOutput(new Slf4jLogConsumer(log).withPrefix("KAFKA"));
                log.info("✅ Kafka iniciado: {}", kafka.getBootstrapServers());
            }

            // PostgreSQL Origem
            if (!postgresSource.isRunning()) {
                postgresSource.start();
                postgresSource.followOutput(new Slf4jLogConsumer(log).withPrefix("PG-SOURCE"));
                log.info("✅ PostgreSQL Origem iniciado: {}", postgresSource.getJdbcUrl());
            }

            // PostgreSQL Destino
            if (!postgresTarget.isRunning()) {
                postgresTarget.start();
                postgresTarget.followOutput(new Slf4jLogConsumer(log).withPrefix("PG-TARGET"));
                log.info("✅ PostgreSQL Destino iniciado: {}", postgresTarget.getJdbcUrl());
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("========================================");
            log.info("Containers iniciados em {} ms", elapsedTime);
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ Erro ao inicializar containers", e);
            // Tentar limpar recursos
            tearDown();
            throw new RuntimeException("Falha ao inicializar ambiente de testes", e);
        }
    }

    /**
     * Teardown executado UMA VEZ após todos os testes da classe
     * Garante que containers sejam parados e recursos liberados
     */
    @AfterAll
    static void tearDown() {
        log.info("========================================");
        log.info("Finalizando containers Testcontainers...");
        log.info("========================================");

        try {
            // Parar containers na ordem inversa de inicialização
            if (kafka != null && kafka.isRunning()) {
                kafka.stop();
                log.info("✅ Kafka parado");
            }

            if (postgresTarget != null && postgresTarget.isRunning()) {
                postgresTarget.stop();
                log.info("✅ PostgreSQL Destino parado");
            }

            if (postgresSource != null && postgresSource.isRunning()) {
                postgresSource.stop();
                log.info("✅ PostgreSQL Origem parado");
            }

            log.info("========================================");
            log.info("Todos os containers foram finalizados");
            log.info("========================================");

        } catch (Exception e) {
            log.error("⚠️ Erro ao finalizar containers (recursos podem não ter sido liberados)", e);
        }
    }

    // ============================================
    // DYNAMIC PROPERTIES CONFIGURATION
    // ============================================

    /**
     * Configura properties dinâmicas do Spring Boot para usar Testcontainers
     * Sobrescreve configurações do application.yml/properties durante testes
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        log.info("Configurando properties dinâmicas do Spring Boot...");

        // ============================================
        // KAFKA CONFIGURATION
        // ============================================
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        
        // Configurações adicionais Kafka para testes
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-consumer-group");
        registry.add("spring.kafka.consumer.enable-auto-commit", () -> "false");

        // ============================================
        // POSTGRESQL SOURCE (Producer CDC)
        // ============================================
        registry.add("spring.datasource.source.url", postgresSource::getJdbcUrl);
        registry.add("spring.datasource.source.username", postgresSource::getUsername);
        registry.add("spring.datasource.source.password", postgresSource::getPassword);
        registry.add("spring.datasource.source.driver-class-name", () -> "org.postgresql.Driver");

        // ============================================
        // POSTGRESQL TARGET (Consumer Persistence)
        // ============================================
        registry.add("spring.datasource.url", postgresTarget::getJdbcUrl);
        registry.add("spring.datasource.username", postgresTarget::getUsername);
        registry.add("spring.datasource.password", postgresTarget::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // ============================================
        // JPA/HIBERNATE CONFIGURATION
        // ============================================
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> "false");  // Reduzir logs em testes
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");

        // ============================================
        // LOGGING (reduzir ruído em testes)
        // ============================================
        registry.add("logging.level.org.hibernate", () -> "WARN");
        registry.add("logging.level.org.springframework.kafka", () -> "WARN");
        registry.add("logging.level.org.apache.kafka", () -> "WARN");
        registry.add("logging.level.com.esocial", () -> "DEBUG");  // Nossos logs em DEBUG

        log.info("✅ Properties dinâmicas configuradas");
    }

    // ============================================
    // HELPER METHODS (para classes filhas)
    // ============================================

    /**
     * Retorna URL de conexão do Kafka para uso direto em testes
     */
    protected static String getKafkaBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    /**
     * Retorna JDBC URL do PostgreSQL Origem
     */
    protected static String getPostgresSourceJdbcUrl() {
        return postgresSource.getJdbcUrl();
    }

    /**
     * Retorna JDBC URL do PostgreSQL Destino
     */
    protected static String getPostgresTargetJdbcUrl() {
        return postgresTarget.getJdbcUrl();
    }

    /**
     * Verifica se todos os containers estão rodando
     * Útil para debugging
     */
    protected static boolean areAllContainersRunning() {
        boolean allRunning = kafka.isRunning() 
                && postgresSource.isRunning() 
                && postgresTarget.isRunning();
        
        if (!allRunning) {
            log.warn("⚠️ Nem todos os containers estão rodando:");
            log.warn("  - Kafka: {}", kafka.isRunning());
            log.warn("  - PostgreSQL Source: {}", postgresSource.isRunning());
            log.warn("  - PostgreSQL Target: {}", postgresTarget.isRunning());
        }
        
        return allRunning;
    }
    // ============================================
    // HELPER METHODS PARA CRIAR EVENTOS DE TESTE
    // ============================================

    /**
     * Cria um evento de INSERT válido para testes
     */
    protected EmployeeEventDTO createValidInsertEvent(String sourceId) {
        return createValidInsertEvent(sourceId, new BigDecimal("5000.00"));
    }

    /**
     * Cria um evento de INSERT válido com salário customizado
     */
    protected EmployeeEventDTO createValidInsertEvent(String sourceId, BigDecimal salary) {
        UUID correlationId = UUID.randomUUID();

        return EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("CREATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf(generateValidCpf())
                .pis(generateValidPis())
                .fullName("Colaborador Teste " + sourceId)
                .birthDate(LocalDate.of(1990, 1, 15))
                .admissionDate(LocalDate.of(2024, 1, 1))
                .jobTitle("Analista")
                .department("TI")
                .salary(salary)  // ← BigDecimal
                .status("ACTIVE")
                .build();
    }

    /**
     * Cria um evento de UPDATE válido
     */
    protected EmployeeEventDTO createValidUpdateEvent(String sourceId, BigDecimal newSalary) {
        UUID correlationId = UUID.randomUUID();

        return EmployeeEventDTO.builder()
                .eventId(correlationId.toString())
                .eventType("UPDATE")
                .eventTimestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .sourceId(sourceId)
                .cpf("12345678901")  // Mesmo CPF
                .pis("10011223344")
                .fullName("Colaborador Teste " + sourceId)
                .birthDate(LocalDate.of(1990, 1, 15))
                .admissionDate(LocalDate.of(2024, 1, 1))
                .jobTitle("Analista Senior")  // Alterado
                .department("TI")
                .salary(newSalary)  // ← BigDecimal alterado
                .status("ACTIVE")
                .build();
    }

    /**
     * Gera CPF válido sequencial para testes
     */
    private static int cpfSequence = 1;
    protected synchronized String generateValidCpf() {
        return String.format("123456789%02d", cpfSequence++);
    }

    /**
     * Gera PIS válido sequencial para testes
     */
    private static int pisSequence = 1;
    protected synchronized String generateValidPis() {
        return String.format("100112233%02d", pisSequence++);
    }

    /**
     * Cria BigDecimal a partir de string (helper)
     */
    protected BigDecimal toBigDecimal(String value) {
        return new BigDecimal(value);
    }

    /**
     * Cria BigDecimal a partir de double (helper)
     */
    protected BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
