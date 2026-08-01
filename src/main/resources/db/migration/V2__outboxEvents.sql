-- Outbox events (transactional outbox pattern for Kafka publishing)
CREATE TABLE outbox_events (
    id                  UUID PRIMARY KEY,
    event_type          VARCHAR(100) NOT NULL,
    payload             JSONB NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    published_at        TIMESTAMPTZ,
    retry_count         INTEGER NOT NULL DEFAULT 0,
    last_error          TEXT,
    last_attempted_at   TIMESTAMPTZ,
    next_retry_at       TIMESTAMPTZ,
    failed_at           TIMESTAMPTZ
);

-- Partial index: only indexes rows still eligible for publishing, so the
-- relay's poll query stays cheap regardless of how many published/failed
-- rows accumulate over time.
CREATE INDEX idx_outbox_events_unpublished
    ON outbox_events (next_retry_at)
    WHERE published_at IS NULL AND failed_at IS NULL;