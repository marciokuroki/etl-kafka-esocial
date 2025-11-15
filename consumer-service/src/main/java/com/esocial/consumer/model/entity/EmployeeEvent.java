package com.esocial.consumer.model.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import com.esocial.consumer.model.entity.enums.EventStatus;
import com.esocial.consumer.model.entity.enums.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee_events", schema = "public", indexes = {
        @Index(name = "idx_employee_events_source_id", columnList = "source_id", unique = true),
        @Index(name = "idx_employee_events_event_id", columnList = "event_id", unique = true),
        @Index(name = "idx_employee_events_event_type", columnList = "event_type"),
        @Index(name = "idx_employee_events_status", columnList = "status"),
        @Index(name = "idx_employee_events_kafka_offset", columnList = "kafka_offset"),
        @Index(name = "idx_employee_events_correlation_id", columnList = "correlation_id"),
        @Index(name = "idx_employee_events_validation_executed_at", columnList = "validation_executed_at"),
        @Index(name = "idx_employee_events_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "source_id", length = 20, nullable = false, unique = true)
    private String sourceId;
    
    @Column(name = "event_id", length = 50, nullable = false, unique = true)
    private String eventId;
    
    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;
    
    @Column(name = "event_type", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private EventStatus status;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
    
    @Column(name = "kafka_topic", length = 100, nullable = false)
    private String kafkaTopic;
    
    @Column(name = "kafka_partition", nullable = false)
    private Integer kafkaPartition;
    
    @Column(name = "kafka_offset", nullable = false)
    private Long kafkaOffset;
    
    @Column(name = "event_payload", columnDefinition = "TEXT")
    private String eventPayload;
    
    @Column(name = "validation_status", length = 20)
    private String validationStatus;
    
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;
    
    @Column(name = "validation_warnings", columnDefinition = "TEXT")
    private String validationWarnings;
    
    @Column(name = "validation_executed_at")
    private LocalDateTime validationExecutedAt;
    
    @Column(name = "processing_status", length = 20)
    private String processingStatus;
    
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;
    
    @Column(name = "processing_finished_at")
    private LocalDateTime processingFinishedAt;
    
    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;
    
    @Column(name = "esocial_sent", columnDefinition = "boolean default false")
    private Boolean esocialSent;
    
    @Column(name = "esocial_sent_at")
    private LocalDateTime esocialSentAt;
    
    @Column(name = "esocial_protocol", length = 100)
    private String esocialProtocol;
    
    @Column(name = "esocial_status", length = 20)
    private String esocialStatus;
    
    @Column(name = "esocial_response", columnDefinition = "TEXT")
    private String esocialResponse;
    
    @Column(name = "retry_count")
    private Integer retryCount;
    
    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "version")
    private Integer version;
    
    @Column(name = "employee_id")
    private Long employeeId;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        version = 1;
        status = EventStatus.RECEIVED;
        validationStatus = "PENDING";
        processingStatus = "PENDING";
        esocialSent = false;
        retryCount = 0;
        if (createdBy == null) {
            createdBy = "SYSTEM";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (version != null) {
            version++;
        }
    }
    
    public boolean isValid() {
        return "PASSED".equals(validationStatus);
    }
    
    public boolean hasFailed() {
        return "FAILED".equals(validationStatus);
    }
    
    public boolean isProcessingComplete() {
        return "COMPLETED".equals(processingStatus);
    }
    
    public boolean hasEsocialResponse() {
        return esocialProtocol != null && !esocialProtocol.trim().isEmpty();
    }
    
    public boolean isAdmissionEvent() {
        return EventType.S_2300 == eventType || EventType.S_2306 == eventType;
    }
    
    public boolean isAlterationEvent() {
        return EventType.S_2400 == eventType || EventType.S_2405 == eventType;
    }
    
    public boolean isTerminationEvent() {
        return EventType.S_2420 == eventType;
    }
    
    public void markAsValidationPassed() {
        this.validationStatus = "PASSED";
        this.validationExecutedAt = LocalDateTime.now();
        transitionTo(EventStatus.VALIDATION_PASSED);
    }
    
    public void markAsValidationFailed(String errorJson) {
        this.validationStatus = "FAILED";
        this.validationErrors = errorJson;
        this.validationExecutedAt = LocalDateTime.now();
        transitionTo(EventStatus.VALIDATION_FAILED);
    }
    
    public void markAsProcessing() {
        this.processingStatus = "PROCESSING";
        this.processingStartedAt = LocalDateTime.now();
        transitionTo(EventStatus.PROCESSING);
    }
    
    public void markAsProcessed() {
        this.processingStatus = "COMPLETED";
        this.processingFinishedAt = LocalDateTime.now();
        if (processingStartedAt != null) {            
            this.processingDurationMs = Duration.between(processingStartedAt, processingFinishedAt).toMillis();
        }
        transitionTo(EventStatus.PROCESSED);
    }
    
    public void markAsProcessingFailed(String error) {
        this.processingStatus = "FAILED";
        this.errorMessage = error;
        this.processingFinishedAt = LocalDateTime.now();
        if (processingStartedAt != null) {            
            this.processingDurationMs = Duration.between(processingStartedAt, processingFinishedAt).toMillis();
        }
        transitionTo(EventStatus.PROCESSING_FAILED);
    }
    
    public void markAsSendingToEsocial() {
        transitionTo(EventStatus.SENDING_TO_ESOCIAL);
    }
    
    public void markAsEsocialSent(String protocol) {
        this.esocialSent = true;
        this.esocialSentAt = LocalDateTime.now();
        this.esocialProtocol = protocol;
        this.esocialStatus = "SENT";
        transitionTo(EventStatus.SENT_TO_ESOCIAL);
    }
    
    public void markAsEsocialAccepted() {
        this.esocialStatus = "RECEIVED";
        transitionTo(EventStatus.ESOCIAL_ACCEPTED);
    }
    
    public void markAsEsocialProcessed() {
        this.esocialStatus = "PROCESSED";
        transitionTo(EventStatus.ESOCIAL_PROCESSED);
    }
    
    public void markAsEsocialRejected(String response) {
        this.esocialStatus = "REJECTED";
        this.esocialResponse = response;
        transitionTo(EventStatus.ESOCIAL_REJECTED);
    }
    
    public void markAsArchived() {
        transitionTo(EventStatus.ARCHIVED);
    }
    
    public void markAsError(String errorMsg) {
        this.errorMessage = errorMsg;
        this.status = EventStatus.ERROR;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void scheduleRetry(int delayMinutes) {
        this.retryCount = (retryCount != null ? retryCount : 0) + 1;
        this.lastRetryAt = LocalDateTime.now();
        this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes);
    }
    
    public void transitionTo(EventStatus newStatus) {
        if (canTransitionTo(newStatus)) {
            this.status = newStatus;
            this.updatedAt = LocalDateTime.now();
        } else {
            throw new IllegalStateException(
                String.format("Transição inválida de %s para %s", this.status, newStatus)
            );
        }
    }
    
    public boolean canTransitionTo(EventStatus newStatus) {
        switch (this.status) {
            case RECEIVED:
                return newStatus == EventStatus.VALIDATING;
            case VALIDATING:
                return newStatus == EventStatus.VALIDATION_PASSED || 
                       newStatus == EventStatus.VALIDATION_FAILED;
            case VALIDATION_PASSED:
                return newStatus == EventStatus.PROCESSING;
            case PROCESSING:
                return newStatus == EventStatus.PROCESSED || 
                       newStatus == EventStatus.PROCESSING_FAILED;
            case PROCESSING_FAILED:
                return newStatus == EventStatus.ERROR;
            case PROCESSED:
                return newStatus == EventStatus.SENDING_TO_ESOCIAL;
            case SENDING_TO_ESOCIAL:
                return newStatus == EventStatus.SENT_TO_ESOCIAL || 
                       newStatus == EventStatus.ERROR;
            case SENT_TO_ESOCIAL:
                return newStatus == EventStatus.ESOCIAL_ACCEPTED || 
                       newStatus == EventStatus.ESOCIAL_REJECTED;
            case ESOCIAL_ACCEPTED:
                return newStatus == EventStatus.ESOCIAL_PROCESSED;
            case ESOCIAL_PROCESSED:
                return newStatus == EventStatus.ARCHIVED;
            case ERROR:
                return newStatus == EventStatus.PROCESSING || 
                       newStatus == EventStatus.ARCHIVED;
            default:
                return false;
        }
    }
    
    public void markAsReceivedFromKafka() {
        this.status = EventStatus.RECEIVED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsValidating() {
        transitionTo(EventStatus.VALIDATING);
    }
}
