package com.ticketing.common.outbox;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String eventType, String payload) {
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.retryCount = 0;
    }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getRetryCount() { return retryCount; }
    public String getLastError() { return lastError; }
    public Instant getLastAttemptedAt() { return lastAttemptedAt; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public Instant getFailedAt() { return failedAt; }

    public boolean isPublished() {
        return publishedAt != null;
    }

    public boolean isFailed() {
        return failedAt != null;
    }

    public void markPublished() {
        if (isPublished()) {
            throw new IllegalStateException("OutboxEvent is already published");
        }
        this.publishedAt = Instant.now();
    }

    public void recordFailure(String errorMessage, int maxRetries, java.time.Duration baseBackoff) {
        if (isPublished()) {
            throw new IllegalStateException("Cannot record a failure on an already-published OutboxEvent");
        }
        if (isFailed()) {
            throw new IllegalStateException("Cannot record a failure on an already dead-lettered OutboxEvent");
        }
        this.retryCount++;
        this.lastError = errorMessage;
        this.lastAttemptedAt = Instant.now();

        if (this.retryCount >= maxRetries) {
            this.failedAt = Instant.now();
            this.nextRetryAt = null;
        } else {
            long backoffSeconds = baseBackoff.toSeconds() * (1L << this.retryCount);
            this.nextRetryAt = Instant.now().plusSeconds(backoffSeconds);
        }
    }
}