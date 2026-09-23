CREATE TABLE notification_outbox (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    channel varchar(20) NOT NULL,
    template_code varchar(120) NOT NULL,
    encrypted_payload text NOT NULL,
    idempotency_key varchar(160) NOT NULL,
    status varchar(20) NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    max_attempts integer NOT NULL,
    next_attempt_at timestamptz NOT NULL,
    expires_at timestamptz,
    sent_at timestamptz,
    provider_message_id varchar(200),
    last_error varchar(120),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_outbox_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_notification_outbox_channel CHECK (channel IN ('SMS', 'EMAIL', 'PUSH', 'IN_APP')),
    CONSTRAINT ck_notification_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY', 'SENT', 'DEAD')),
    CONSTRAINT ck_notification_outbox_attempts CHECK (attempt_count >= 0 AND max_attempts > 0)
);

CREATE INDEX ix_notification_outbox_due
    ON notification_outbox(status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'PROCESSING', 'RETRY');

CREATE INDEX ix_notification_outbox_expiry
    ON notification_outbox(expires_at)
    WHERE status IN ('PENDING', 'PROCESSING', 'RETRY') AND expires_at IS NOT NULL;
