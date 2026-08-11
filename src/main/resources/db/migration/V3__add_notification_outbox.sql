SET search_path TO agendaflow, public;

CREATE TABLE notification_outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(254) NOT NULL,
    locale VARCHAR(35) NOT NULL DEFAULT 'en-US',
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_started_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_outbox_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT chk_notification_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'EXHAUSTED')),
    CONSTRAINT chk_notification_outbox_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_notification_outbox_status_attempt
    ON notification_outbox (status, next_attempt_at);

CREATE INDEX idx_notification_outbox_aggregate
    ON notification_outbox (aggregate_type, aggregate_id);

CREATE INDEX idx_notification_outbox_created_at
    ON notification_outbox (created_at);
