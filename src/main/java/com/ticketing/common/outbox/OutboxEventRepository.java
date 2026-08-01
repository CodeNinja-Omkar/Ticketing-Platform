package com.ticketing.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Polls for outbox rows eligible for publishing: not yet published, not
     * dead-lettered, and either never attempted or past their backoff window.
     *
     * FOR UPDATE SKIP LOCKED is required here, and JPQL has no equivalent for
     * SKIP LOCKED, so this must be a native query. It is what allows multiple
     * relay instances to poll the same table concurrently without selecting
     * the same rows: each poll locks the rows it selects, and any other
     * concurrent poll simply skips rows already locked rather than blocking
     * on them.
     *
     * ORDER BY created_at is best-effort FIFO ordering across event types; it
     * is not a strict global ordering guarantee once more than one relay
     * instance is running concurrently.
     */
    @Query(value = """
        SELECT * FROM outbox_events
        WHERE published_at IS NULL
          AND failed_at IS NULL
          AND (next_retry_at IS NULL OR next_retry_at <= :now)
        ORDER BY created_at
        FOR UPDATE SKIP LOCKED
        LIMIT :batchSize
        """, nativeQuery = true)
    List<OutboxEvent> pollBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);
}