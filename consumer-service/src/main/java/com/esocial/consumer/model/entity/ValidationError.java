package com.esocial.consumer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "validation_errors", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", length = 50)
    private String eventId;
    
    @Column(name = "source_table", length = 50)
    private String sourceTable;
    
    @Column(name = "source_id", length = 50)
    private String sourceId;
    
    @Column(name = "validation_rule", length = 100, nullable = false)
    private String validationRule;
    
    @Column(name = "error_message", columnDefinition = "TEXT", nullable = false)
    private String errorMessage;
    
    @Column(name = "severity", length = 20, nullable = false)
    private String severity; // ERROR, WARNING, INFO
    
    @Column(name = "field_name", length = 100)
    private String fieldName;
    
    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;
    
    @Column(name = "event_payload", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String eventPayload;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;
    
    @Column(name = "kafka_offset")
    private Long kafkaOffset;
    
    @Column(name = "kafka_partition")
    private Integer kafkaPartition;
    
    @Column(name = "kafka_topic", length = 100)
    private String kafkaTopic;
    
    @Column(name = "correlation_id")
    private UUID correlationId;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
