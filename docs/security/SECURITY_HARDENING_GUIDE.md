# Security Hardening Guide - Pipeline ETL eSocial

**Versão:** 1.0  
**Data:** 2025-11-22  
**Responsável:** Márcio Kuroki Gonçalves  
**Criticidade:** 🔴 ALTA - Bloqueador para produção

---

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Autenticação Kafka (SASL/SCRAM)](#autenticação-kafka-saslscram)
3. [Criptografia TLS/SSL](#criptografia-tlsssl)
4. [Criptografia de Dados Sensíveis](#criptografia-de-dados-sensíveis)
5. [Gestão de Secrets](#gestão-de-secrets)
6. [Rate Limiting e Proteção DDoS](#rate-limiting-e-proteção-ddos)
7. [CORS e Segurança de APIs](#cors-e-segurança-de-apis)
8. [Scan de Vulnerabilidades](#scan-de-vulnerabilidades)
9. [Auditoria e Compliance](#auditoria-e-compliance)
10. [Checklist Final](#checklist-final)

---

## Visão Geral

### Estado Atual (Inseguro) ❌

| Componente | Vulnerabilidade | Risco |
|------------|-----------------|-------|
| **Kafka** | Sem autenticação | Qualquer um pode publicar/consumir |
| **Kafka** | Sem TLS | Dados trafegam em texto plano |
| **PostgreSQL** | Senha hardcoded | Exposição em repositório Git |
| **APIs REST** | Sem rate limiting | Vulnerável a DDoS |
| **APIs REST** | CORS aberto | Acesso de qualquer origem |
| **Dados Sensíveis** | CPF/PIS em texto plano | Não conformidade LGPD |
| **Dependências** | Não verificadas | Vulnerabilidades conhecidas (CVEs) |

**Nota para o TCC:** Este é um projeto acadêmico. Em produção real, estas vulnerabilidades seriam **bloqueadores absolutos**.

---

### Estado Desejado (Seguro) ✅

| Componente | Implementação | Benefit |
|------------|---------------|---------|
| **Kafka** | SASL/SCRAM-SHA-256 | Autenticação forte |
| **Kafka** | TLS 1.3 | Criptografia em trânsito |
| **PostgreSQL** | AWS Secrets Manager | Rotação automática de senhas |
| **APIs REST** | Bucket4j (rate limiting) | Proteção DDoS |
| **APIs REST** | CORS restrito | Apenas origens conhecidas |
| **Dados Sensíveis** | AES-256-GCM | Criptografia at-rest |
| **Dependências** | OWASP Dependency-Check | CVE scanning |

---

## Autenticação Kafka (SASL/SCRAM)

### Por Que SASL/SCRAM?

**SASL** (Simple Authentication and Security Layer): Framework de autenticação  
**SCRAM** (Salted Challenge Response Authentication Mechanism): Protocolo seguro com hashing

**Alternativas descartadas:**
- ❌ **PLAIN:** Senha em texto plano (inseguro)
- ❌ **GSSAPI/Kerberos:** Complexidade operacional alta
- ✅ **SCRAM-SHA-256:** Balance segurança/simplicidade

---

### Passo 1: Configurar Usuários SASL

**Criar usuários no Zookeeper:**

```


# Conectar no container Zookeeper

docker exec -it esocial-zookeeper bash

# Criar usuário admin

kafka-configs --zookeeper localhost:2181 --alter \
--add-config 'SCRAM-SHA-256=[password=admin-secret-password]' \
--entity-type users --entity-name admin

# Criar usuário producer

kafka-configs --zookeeper localhost:2181 --alter \
--add-config 'SCRAM-SHA-256=[password=producer-secret-password]' \
--entity-type users --entity-name producer-user

# Criar usuário consumer

kafka-configs --zookeeper localhost:2181 --alter \
--add-config 'SCRAM-SHA-256=[password=consumer-secret-password]' \
--entity-type users --entity-name consumer-user

# Verificar usuários criados

kafka-configs --zookeeper localhost:2181 --describe \
--entity-type users --entity-name admin

```

---

### Passo 2: Configurar Brokers Kafka

**Atualizar `docker-compose.yml`:**

```

services:
kafka-broker-1:
image: confluentinc/cp-kafka:7.5.0
environment:
\# ... outras configurações

      # SASL Configuration
      KAFKA_SASL_ENABLED_MECHANISMS: SCRAM-SHA-256
      KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL: SCRAM-SHA-256
      
      # Listener Configuration
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: |
        INTERNAL:SASL_PLAINTEXT,
        EXTERNAL:SASL_PLAINTEXT
      
      KAFKA_LISTENERS: |
        INTERNAL://0.0.0.0:9092,
        EXTERNAL://0.0.0.0:19092
      
      KAFKA_ADVERTISED_LISTENERS: |
        INTERNAL://kafka-broker-1:9092,
        EXTERNAL://localhost:19092
      
      # Inter-broker authentication
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_SASL_JAAS_CONFIG: |
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="admin"
        password="admin-secret-password";
      
      # Zookeeper SASL (se necessário)
      KAFKA_ZOOKEEPER_SET_ACL: "true"
    ```

**Reiniciar brokers:**

```

docker-compose restart kafka-broker-1 kafka-broker-2 kafka-broker-3

```

---

### Passo 3: Configurar Producer Service

**Adicionar configuração SASL em `application.yml`:**

```


# producer-service/src/main/resources/application.yml

spring:
kafka:
bootstrap-servers: \${KAFKA_BOOTSTRAP_SERVERS:localhost:19092}

    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      
      properties:
        # SASL Configuration
        security.protocol: SASL_PLAINTEXT  # Trocar para SASL_SSL quando TLS estiver habilitado
        sasl.mechanism: SCRAM-SHA-256
        sasl.jaas.config: |
          org.apache.kafka.common.security.scram.ScramLoginModule required
          username="${KAFKA_SASL_USERNAME:producer-user}"
          password="${KAFKA_SASL_PASSWORD:producer-secret-password}";
    ```

**Usar variáveis de ambiente (não hardcode):**

```


# .env

KAFKA_SASL_USERNAME=producer-user
KAFKA_SASL_PASSWORD=producer-secret-password  \# Em produção: AWS Secrets Manager

```

---

### Passo 4: Configurar Consumer Service

```


# consumer-service/src/main/resources/application.yml

spring:
kafka:
bootstrap-servers: \${KAFKA_BOOTSTRAP_SERVERS:localhost:19092}

    consumer:
      group-id: esocial-consumer-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      
      properties:
        # SASL Configuration
        security.protocol: SASL_PLAINTEXT
        sasl.mechanism: SCRAM-SHA-256
        sasl.jaas.config: |
          org.apache.kafka.common.security.scram.ScramLoginModule required
          username="${KAFKA_SASL_USERNAME:consumer-user}"
          password="${KAFKA_SASL_PASSWORD:consumer-secret-password}";
    ```

---

### Passo 5: Testar Autenticação

**Teste positivo (credenciais corretas):**

```


# Publicar mensagem com autenticação

docker exec esocial-producer-service \
kafka-console-producer \
--bootstrap-server kafka-broker-1:9092 \
--topic employee-create \
--producer-property 'security.protocol=SASL_PLAINTEXT' \
--producer-property 'sasl.mechanism=SCRAM-SHA-256' \
--producer-property 'sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="producer-user" password="producer-secret-password";'

# Digite uma mensagem de teste

{"test": "authenticated message"}

# Sucesso: Mensagem enviada

```

**Teste negativo (credenciais incorretas):**

```


# Tentar sem autenticação (deve falhar)

kafka-console-producer \
--bootstrap-server localhost:19092 \
--topic employee-create

# Erro esperado:

# ERROR Authentication failed during authentication due to invalid credentials

```

✅ **Autenticação funcionando corretamente!**

---

## Criptografia TLS/SSL

### Passo 1: Gerar Certificados SSL

**Script: `scripts/generate-ssl-certs.sh`**

```

\#!/bin/bash

# Gerar certificados SSL para Kafka

CERT_DIR="./ssl-certs"
VALIDITY_DAYS=3650  \# 10 anos (projeto acadêmico)

mkdir -p \$CERT_DIR

# 1. Criar CA (Certificate Authority)

openssl req -new -x509 \
-keyout \$CERT_DIR/ca-key \
-out \$CERT_DIR/ca-cert \
-days \$VALIDITY_DAYS \
-subj "/CN=EsocialCA" \
-passout pass:ca-password

# 2. Criar keystore para cada broker

for i in 1 2 3; do
echo "Gerando certificado para broker-\$i..."

# Criar keystore

keytool -genkey -keystore $CERT_DIR/kafka-broker-$i.keystore.jks \
-validity $VALIDITY_DAYS \
    -storepass broker-password \
    -keypass broker-password \
    -dname "CN=kafka-broker-$i" \
-storetype pkcs12

# Criar CSR (Certificate Signing Request)

keytool -keystore $CERT_DIR/kafka-broker-$i.keystore.jks \
-certreq -file $CERT_DIR/broker-$i-cert-request \
-storepass broker-password

# Assinar CSR com CA

openssl x509 -req \
-CA \$CERT_DIR/ca-cert \
-CAkey \$CERT_DIR/ca-key \
-in $CERT_DIR/broker-$i-cert-request \
-out $CERT_DIR/broker-$i-cert-signed \
-days \$VALIDITY_DAYS \
-CAcreateserial \
-passin pass:ca-password

# Importar CA para keystore

keytool -keystore $CERT_DIR/kafka-broker-$i.keystore.jks \
-import -file \$CERT_DIR/ca-cert \
-storepass broker-password \
-noprompt

# Importar certificado assinado

keytool -keystore $CERT_DIR/kafka-broker-$i.keystore.jks \
-import -file $CERT_DIR/broker-$i-cert-signed \
-storepass broker-password \
-noprompt

# Criar truststore

keytool -keystore $CERT_DIR/kafka-broker-$i.truststore.jks \
-import -file \$CERT_DIR/ca-cert \
-storepass broker-password \
-noprompt
done

# 3. Criar truststore para clientes (Producer/Consumer)

keytool -keystore \$CERT_DIR/client.truststore.jks \
-import -file \$CERT_DIR/ca-cert \
-storepass client-password \
-noprompt

echo "Certificados SSL gerados com sucesso em \$CERT_DIR!"

```

**Executar:**

```

chmod +x scripts/generate-ssl-certs.sh
./scripts/generate-ssl-certs.sh

```

---

### Passo 2: Configurar TLS nos Brokers

**Atualizar `docker-compose.yml`:**

```

services:
kafka-broker-1:
image: confluentinc/cp-kafka:7.5.0
volumes:
- ./ssl-certs:/etc/kafka/secrets
environment:
\# ... configurações anteriores

      # TLS/SSL Configuration
      KAFKA_SSL_KEYSTORE_LOCATION: /etc/kafka/secrets/kafka-broker-1.keystore.jks
      KAFKA_SSL_KEYSTORE_PASSWORD: broker-password
      KAFKA_SSL_KEY_PASSWORD: broker-password
      KAFKA_SSL_TRUSTSTORE_LOCATION: /etc/kafka/secrets/kafka-broker-1.truststore.jks
      KAFKA_SSL_TRUSTSTORE_PASSWORD: broker-password
      
      # Update listener security protocol
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: |
        INTERNAL:SASL_SSL,
        EXTERNAL:SASL_SSL
      
      # TLS versions
      KAFKA_SSL_ENABLED_PROTOCOLS: TLSv1.3,TLSv1.2
      KAFKA_SSL_PROTOCOL: TLSv1.3
    ```

---

### Passo 3: Configurar TLS nos Clientes

**Producer Service:**

```


# producer-service/src/main/resources/application.yml

spring:
kafka:
producer:
properties:
security.protocol: SASL_SSL  \# ← Mudou de SASL_PLAINTEXT
sasl.mechanism: SCRAM-SHA-256
sasl.jaas.config: |
org.apache.kafka.common.security.scram.ScramLoginModule required
username="${KAFKA_SASL_USERNAME}"
          password="${KAFKA_SASL_PASSWORD}";

        # TLS Configuration
        ssl.truststore.location: /etc/kafka/secrets/client.truststore.jks
        ssl.truststore.password: ${SSL_TRUSTSTORE_PASSWORD:client-password}
        ssl.enabled.protocols: TLSv1.3
    ```

**Dockerfile atualizado:**

```


# producer-service/Dockerfile

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/target/*.jar app.jar
COPY ssl-certs/client.truststore.jks /etc/kafka/secrets/client.truststore.jks
ENTRYPOINT ["java", "-jar", "/app.jar"]

```

---

### Passo 4: Testar TLS

**Verificar conexão TLS:**

```


# Conectar com openssl

openssl s_client -connect localhost:19092 -tls1_3

# Saída esperada:

# SSL handshake has read 1234 bytes and written 567 bytes

# New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384

# Server certificate:

# subject=/CN=kafka-broker-1

# issuer=/CN=EsocialCA

# SSL-Session:

# Protocol  : TLSv1.3

# Cipher    : TLS_AES_256_GCM_SHA384

```

✅ **TLS habilitado com sucesso!**

---

## Criptografia de Dados Sensíveis

### Estratégia: Criptografia Transparente (At-Rest)

**Campos a criptografar:**
- CPF (11 dígitos)
- PIS/PASEP (11 dígitos)
- Salário (valores monetários)
- Email (se sensível)

### Passo 1: Adicionar Dependência de Criptografia

```

<!-- pom.xml -->
<dependency>
    <groupId>org.jasypt</groupId>
    ```
    <artifactId>jasypt-spring-boot-starter</artifactId>
    ```
    <version>3.0.5</version>
</dependency>
```

---

### Passo 2: Configurar Jasypt

```


# application.yml

jasypt:
encryptor:
algorithm: PBEWithHMACSHA512AndAES_256  \# AES-256
key-obtention-iterations: 1000
pool-size: 1
provider-name: SunJCE
salt-generator-classname: org.jasypt.salt.RandomSaltGenerator
iv-generator-classname: org.jasypt.iv.RandomIvGenerator
string-output-type: base64
password: \${JASYPT_ENCRYPTOR_PASSWORD}  \# Variável de ambiente

```

---

### Passo 3: Anotar Entidades

```

// consumer-service/src/main/java/com/esocial/consumer/model/Employee.java

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import javax.persistence.Convert;

@Entity
@Table(name = "employees")
@Data
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "source_id")
    private String sourceId;
    
    @Convert(converter = CpfEncryptionConverter.class)  // ← Criptografia
    @Column(name = "cpf", length = 500)  // Aumentar tamanho (base64)
    private String cpf;
    
    @Convert(converter = PisEncryptionConverter.class)  // ← Criptografia
    @Column(name = "pis", length = 500)
    private String pis;
    
    @Column(name = "full_name")
    private String fullName;  // Nome NÃO é criptografado (buscável)
    
    @Convert(converter = SalaryEncryptionConverter.class)  // ← Criptografia
    @Column(name = "salary", precision = 10, scale = 2)
    private BigDecimal salary;
    
    // ... outros campos
    }

```

---

### Passo 4: Criar Converters JPA

```

// consumer-service/src/main/java/com/esocial/consumer/config/CpfEncryptionConverter.java

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class CpfEncryptionConverter implements AttributeConverter<String, String> {

    private final StandardPBEStringEncryptor encryptor;
    
    public CpfEncryptionConverter() {
        this.encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(System.getenv("JASYPT_ENCRYPTOR_PASSWORD"));
        encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
    }
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return encryptor.encrypt(attribute);  // Criptografar antes de salvar
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return encryptor.decrypt(dbData);  // Descriptografar ao ler
    }
    }

```

**Criar converters similares:**
- `PisEncryptionConverter`
- `SalaryEncryptionConverter`

---

### Passo 5: Testar Criptografia

```


# 1. Definir chave de criptografia

export JASYPT_ENCRYPTOR_PASSWORD="super-secret-key-2025"

# 2. Iniciar serviço

docker-compose up -d consumer-service

# 3. Inserir dado sensível

curl -X POST http://localhost:8082/test-encryption \
-H "Content-Type: application/json" \
-d '{
"cpf": "12345678901",
"pis": "10011223344",
"salary": 5500.00
}'

# 4. Verificar no banco (deve estar criptografado)

docker exec -it esocial-postgres-db psql -U esocial_user -d esocial -c \
"SELECT cpf, pis, salary FROM public.employees LIMIT 1;"

# Saída (criptografado):

# cpf                                    | pis                                    | salary

# ----------------------------------------------------------------------------------------------

# aGVsbG8gd29ybGQgdGhpcyBpcyBhIHRlc3Q=   | YW5vdGhlciBlbmNyeXB0ZWQgdmFsdWU=        | dGVzdCBlbmNyeXB0ZWQgc2FsYXJ5

```

**5. Buscar (descriptografia automática):**

```

curl http://localhost:8082/api/v1/employees/1 | jq

# Saída (descriptografado automaticamente pelo converter):

# {

# "id": 1,

# "cpf": "12345678901",  ← Descriptografado

# "pis": "10011223344",  ← Descriptografado

# "salary": 5500.00      ← Descriptografado

# }

```

✅ **Criptografia funcionando! CPF/PIS protegidos no banco.**

---

## Gestão de Secrets

### Opção 1: Docker Secrets (Desenvolvimento)

```


# docker-compose.yml

secrets:
kafka_producer_password:
file: ./secrets/kafka-producer-password.txt
kafka_consumer_password:
file: ./secrets/kafka-consumer-password.txt
postgres_password:
file: ./secrets/postgres-password.txt

services:
producer-service:
secrets:
- kafka_producer_password
environment:
KAFKA_SASL_PASSWORD_FILE: /run/secrets/kafka_producer_password

```

**Ler secret no código:**

```

// ProducerConfig.java

@Value("\${KAFKA_SASL_PASSWORD_FILE:/run/secrets/kafka_producer_password}")
private String kafkaPasswordFile;

@Bean
public String kafkaPassword() throws IOException {
return Files.readString(Paths.get(kafkaPasswordFile)).trim();
}

```

---

### Opção 2: AWS Secrets Manager (Produção)

**Adicionar dependência:**

```

<dependency>
    <groupId>com.amazonaws.secretsmanager</groupId>
    ```
    <artifactId>aws-secretsmanager-jdbc</artifactId>
    ```
    <version>1.0.6</version>
</dependency>
```

**Configurar:**

```


# application.yml

aws:
secrets-manager:
enabled: true
region: us-east-1
secrets:
- name: esocial/kafka/producer
keys:
- username
- password
- name: esocial/postgres/credentials
keys:
- username
- password

spring:
datasource:
url: jdbc-secretsmanager:postgresql://localhost:5432/esocial
username: \${aws.secrets-manager.secrets.username}[^1]
password: \${aws.secrets-manager.secrets.password}[^1]

```

**Configurar IAM Role:**

```

{
"Version": "2012-10-17",
"Statement": [
{
"Effect": "Allow",
"Action": [
"secretsmanager:GetSecretValue"
],
"Resource": "arn:aws:secretsmanager:us-east-1:123456789012:secret:esocial/*"
}
]
}

```

✅ **Secrets centralizados e rotacionados automaticamente!**

---

## Rate Limiting e Proteção DDoS

### Implementar Bucket4j (Token Bucket Algorithm)

**Adicionar dependência:**

```

<dependency>
    ```
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    ```
    ```
    <artifactId>bucket4j-core</artifactId>
    ```
    <version>8.5.0</version>
</dependency>
```

**Configurar Rate Limiter:**

```

// consumer-service/src/main/java/com/esocial/consumer/config/RateLimitConfig.java

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Bucket apiRateLimiter() {
        // Limite: 100 requisições por minuto por IP
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
    }

```

**Criar Interceptor:**

```

// consumer-service/src/main/java/com/esocial/consumer/config/RateLimitInterceptor.java

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Bucket bucket;
    
    @Autowired
    public RateLimitInterceptor(Bucket bucket) {
        this.bucket = bucket;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-RateLimit-Retry-After-Seconds", 
                String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));
            return false;
        }
    }
    }

```

**Registrar Interceptor:**

```

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**");  // Aplicar em todas APIs
    }
    }

```

**Testar:**

```


# Enviar 101 requisições rápidas (deve bloquear a 101ª)

for i in {1..101}; do
curl -s -o /dev/null -w "Request \$i: %{http_code}\n" http://localhost:8082/api/v1/validation/dashboard
done

# Saída esperada:

# Request 1: 200

# Request 2: 200

# ...

# Request 100: 200

# Request 101: 429  ← Too Many Requests

```

✅ **Rate Limiting funcionando!**

---

## CORS e Segurança de APIs

### Configurar CORS Restrito

```

// consumer-service/src/main/java/com/esocial/consumer/config/SecurityConfig.java

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origens permitidas (NÃO usar "*" em produção!)
        configuration.setAllowedOrigins(Arrays.asList(
            "https://dashboard.esocial.empresa.com",
            "https://admin.esocial.empresa.com"
        ));
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Request-ID"));
        
        // Permitir credenciais
        configuration.setAllowCredentials(true);
        
        // Tempo de cache preflight (1 hora)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .csrf().disable()  // Desabilitar CSRF (APIs REST stateless)
            .authorizeHttpRequests()
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()  // Health checks públicos
                .requestMatchers("/api/**").authenticated()  // APIs requerem autenticação
                .anyRequest().denyAll()
            .and()
            .httpBasic();  // Autenticação HTTP Basic (trocar por JWT em produção)
        
        return http.build();
    }
    }

```

**Adicionar autenticação básica (temporária):**

```


# application.yml

spring:
security:
user:
name: \${API_USERNAME:admin}
password: \${API_PASSWORD:admin-password}

```

**Testar CORS:**

```


# Requisição de origem não permitida (deve bloquear)

curl -H "Origin: https://hacker-site.com" \
-H "Access-Control-Request-Method: GET" \
-X OPTIONS http://localhost:8082/api/v1/validation/dashboard

# Resposta: 403 Forbidden

# Requisição de origem permitida (deve permitir)

curl -H "Origin: https://dashboard.esocial.empresa.com" \
-H "Access-Control-Request-Method: GET" \
-X OPTIONS http://localhost:8082/api/v1/validation/dashboard

# Resposta: 200 OK + headers CORS

```

✅ **CORS configurado com restrições!**

---

## Scan de Vulnerabilidades

### OWASP Dependency-Check

**Adicionar plugin Maven:**

```

<!-- pom.xml -->
<build>
  <plugins>
    <plugin>
      <groupId>org.owasp</groupId>
      ```
      <artifactId>dependency-check-maven</artifactId>
      ```
      <version>9.0.7</version>
      <executions>
        <execution>
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        ```
        <failBuildOnCVSS>7</failBuildOnCVSS>  <!-- Falhar se CVSS >= 7 (HIGH) -->
        ```
        <suppressionFiles>
          ```
          <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
          ```
        </suppressionFiles>
      </configuration>
    </plugin>
  </plugins>
</build>
```

**Executar scan:**

```

cd producer-service
mvn dependency-check:check

# Saída:

# [INFO] Checking for updates

# [INFO] Analysis Started

# [INFO] Analyzing dependencies...

# [INFO] Generating report...

# [INFO] Found 3 vulnerabilities

# [ERROR] CVE-2023-12345 - spring-.0.10 (CVSS: 7.5 HIGH)

# [ERROR] CVE-2023-67890 - logback-classic:1.4.11 (CVSS: 8.2 HIGH)

```

**Corrigir vulnerabilidades:**

```

<!-- Atualizar versões vulneráveis -->
<dependency>
    <groupId>org.springframework</groupId>
    ```
    <artifactId>spring-web</artifactId>
    ```
    ```
    <version>6.1.2</version>  <!-- Atualizar para versão sem CVE -->
    ```
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    ```
    <artifactId>logback-classic</artifactId>
    ```
    ```
    <version>1.4.14</version>  <!-- Atualizar -->
    ```
</dependency>
```

**Re-executar scan:**

```

mvn dependency-check:check

# Saída:

# [INFO] Found 0 vulnerabilities

# [INFO] BUILD SUCCESS ✅

```

---

### Trivy (Container Image Scanning)

**Escanear imagem Docker:**

```


# Instalar Trivy

brew install aquasecurity/trivy/trivy  \# macOS

# ou: apt-get install trivy  \# Linux

# Escanear imagem do Producer

docker build -t esocial-producer:latest producer-service/
trivy image esocial-producer:latest

# Saída:

# Total: 12 (UNKNOWN: 0, LOW: 5, MEDIUM: 4, HIGH: 2, CRITICAL: 1)

# Listar apenas HIGH e CRITICAL

trivy image --severity HIGH,CRITICAL esocial-producer:latest

```

**Corrigir vulnerabilidades (atualizar imagem base):**

```


# Dockerfile

# Antes (vulnerável):

FROM openjdk:21-jdk-alpine

# Depois (sem vulnerabilidades):

FROM eclipse-temurin:21-jre-alpine  \# Imagem mantida e atualizada

```

✅ **Vulnerabilidades corrigidas!**

---

## Auditoria e Compliance

### Registro de Acessos (Access Log)

```

// consumer-service/src/main/java/com/esocial/consumer/config/AuditInterceptor.java

@Component
@Slf4j
public class AuditInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String username = request.getRemoteUser();  // Usuário autenticado
        String ip = request.getRemoteAddr();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");
        
        log.info("API_ACCESS: user={}, ip={}, method={}, uri={}, userAgent={}", 
            username, ip, method, uri, userAgent);
        
        return true;
    }
    }

```

**Log estruturado gerado:**

```

{
"timestamp": "2025-11-22T22:30:15.123Z",
"level": "INFO",
"message": "API_ACCESS: user=admin, ip=192.168.1.100, method=GET, uri=/api/v1/validation/dashboard, userAgent=Mozilla/5.0"
}

```

---

### Compliance LGPD

**Implementar anonimização de dados:**

```

// Serviço de anonimização para ambientes de teste

@Service
public class DataAnonymizationService {

    public Employee anonymize(Employee employee) {
        employee.setCpf(generateFakeCpf());
        employee.setPis(generateFakePis());
        employee.setFullName(generateFakeName());
        employee.setEmail(generateFakeEmail());
        // Manter outros campos (estrutura)
        return employee;
    }
    
    private String generateFakeCpf() {
        return String.format("%011d", new Random().nextInt(99999999));
    }
    }

```

**Script de anonimização para ambiente de dev:**

```

-- scripts/anonymize-dev-data.sql

-- Anonimizar CPFs (manter estrutura)
UPDATE public.employees
SET cpf = CONCAT('000', LPAD(id::text, 8, '0'));

-- Anonimizar PISlabel PIS
UPDATE public.employees
SET pis = CONCAT('100', LPAD(id::text, 8, '0'));

-- Anonimizar nomes
UPDATE public.employees
SET full_name = CONCAT('Usuario Teste ', id);

```

✅ **Conformidade LGPD garantida em ambientes não-produção!**

---

## Checklist Final

### ✅ Security Hardening Completo

| Item | Status | Evidência |
|------|--------|-----------|
| ✅ **Autenticação Kafka (SASL/SCRAM)** | COMPLETO | Usuários criados, testes OK |
| ✅ **TLS/SSL Kafka** | COMPLETO | Certificados gerados, TLS 1.3 ativo |
| ✅ **Criptografia dados sensíveis** | COMPLETO | CPF/PIS/Salário criptografados (AES-256) |
| ✅ **Secrets Manager** | COMPLETO | Docker Secrets (dev), AWS SM (prod) |
| ✅ **Rate Limiting APIs** | COMPLETO | Bucket4j (100 req/min) |
| ✅ **CORS configurado** | COMPLETO | Origens restritas |
| ✅ **Scan vulnerabilidades** | COMPLETO | OWASP + Trivy, 0 vulns HIGH/CRITICAL |
| ✅ **Documentação segurança** | COMPLETO | Este guia |

---

### Teste de Penetração Básico (Opcional)

**Ferramenta: OWASP ZAP**

```


# Executar ZAP em modo headless

docker run -t owasp/zap2docker-stable zap-baseline.py \
-t http://localhost:8082 \
-r zap-report.html

# Resultado esperado: 0 vulnerabilidades HIGH

```

---

## Próximos Passos (Produção Real)

1. **Implementar autenticação JWT** (trocar HTTP Basic)
2. **Configurar WAF** (Web Application Firewall)
3. **Habilitar 2FA** (autenticação de dois fatores)
4. **Implementar honeypots** (detectar ataques)
5. **Contratar pentesting profissional**
6. **Obter certificações:** ISO 27001, SOC 2

---

**Última atualização:** 2025-11-22  
**Responsável:** Márcio Kuroki Gonçalves  