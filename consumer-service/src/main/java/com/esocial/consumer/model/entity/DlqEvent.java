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
@Table(name = "dlq_events", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", length = 50, nullable = false)
    private String eventId;
    
    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;
    
    @Column(name = "source_table", length = 50)
    private String sourceTable;
    
    @Column(name = "source_id", length = 50)
    private String sourceId;
    
    @Column(name = "event_payload", columnDefinition = "JSONB", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String eventPayload;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;
    
    @Column(name = "retry_count")
    private Integer retryCount;
    
    @Column(name = "max_retries")
    private Integer maxRetries;
    
    @Column(name = "status", length = 20)
    private String status; // PENDING, REPROCESSING, RESOLVED, DISCARDED
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;
    
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
        retryCount = 0;
        maxRetries = 3;
        status = "PENDING";
    }
}
