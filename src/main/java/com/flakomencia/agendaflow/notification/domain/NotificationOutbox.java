package com.flakomencia.agendaflow.notification.domain;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_outbox", schema = "agendaflow")
public class NotificationOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId;
    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 40)
    private String aggregateType;
    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private Long aggregateId;
    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType;
    @Column(nullable = false, updatable = false, length = 254)
    private String recipient;
    @Column(nullable = false, updatable = false, length = 35)
    private String locale;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationOutboxStatus status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;
    @Column(name = "processing_started_at")
    private OffsetDateTime processingStartedAt;
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
    @Column(name = "last_error", length = 1000)
    private String lastError;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected NotificationOutbox() {}

    public NotificationOutbox(Long organizationId, Long aggregateId, AppointmentNotificationEventType eventType,
            String recipient, String locale, OffsetDateTime now) {
        this.organizationId = organizationId;
        this.aggregateType = "APPOINTMENT";
        this.aggregateId = aggregateId;
        this.eventType = eventType.name();
        this.recipient = recipient;
        this.locale = locale;
        this.payload = "{}";
        this.status = NotificationOutboxStatus.PENDING;
        this.nextAttemptAt = now;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getAggregateType() { return aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getRecipient() { return recipient; }
    public String getLocale() { return locale; }
    public String getPayload() { return payload; }
    public NotificationOutboxStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public OffsetDateTime getNextAttemptAt() { return nextAttemptAt; }
    public OffsetDateTime getProcessingStartedAt() { return processingStartedAt; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public String getLastError() { return lastError; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setPayload(String payload) { this.payload = payload; }
    public void claim(OffsetDateTime now) {
        status = NotificationOutboxStatus.PROCESSING;
        attemptCount++;
        processingStartedAt = now;
        lastError = null;
        updatedAt = now;
    }
    public void publish(OffsetDateTime now) {
        status = NotificationOutboxStatus.PUBLISHED;
        publishedAt = now;
        processingStartedAt = null;
        lastError = null;
        updatedAt = now;
    }
    public void fail(OffsetDateTime now, OffsetDateTime retryAt, String error, int maximumAttempts) {
        status = attemptCount >= maximumAttempts ? NotificationOutboxStatus.EXHAUSTED : NotificationOutboxStatus.PENDING;
        nextAttemptAt = retryAt;
        processingStartedAt = null;
        lastError = error;
        updatedAt = now;
    }
    public void recover(OffsetDateTime now) {
        status = NotificationOutboxStatus.PENDING;
        nextAttemptAt = now;
        processingStartedAt = null;
        lastError = "Recovered stale processing claim";
        updatedAt = now;
    }
}
