CREATE TABLE inquiry_request (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    inquiry_code VARCHAR(100) NOT NULL,
    national_code VARCHAR(10) NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider_code VARCHAR(100),
    external_tracking_code VARCHAR(150),
    facts_json TEXT,
    rejection_code VARCHAR(100),
    result_message VARCHAR(1000),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processing_started_at TIMESTAMP WITH TIME ZONE,
    callback_transport VARCHAR(50) NOT NULL,
    callback_destination VARCHAR(150) NOT NULL,
    callback_correlation_id VARCHAR(150) NOT NULL,
    callback_status VARCHAR(40) NOT NULL,
    callback_attempt_count INTEGER NOT NULL DEFAULT 0,
    callback_next_attempt_at TIMESTAMP WITH TIME ZONE,
    callback_processing_started_at TIMESTAMP WITH TIME ZONE,
    callback_error VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_inquiry_callback_identity UNIQUE (
        callback_transport, callback_destination, callback_correlation_id
    ),
    CONSTRAINT ck_inquiry_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_inquiry_callback_attempt_count CHECK (callback_attempt_count >= 0)
);

CREATE INDEX ix_inquiry_request_due ON inquiry_request(status, next_attempt_at);
CREATE INDEX ix_inquiry_callback_due ON inquiry_request(callback_status, callback_next_attempt_at);

INSERT INTO integration_provider_route (
    id, capability_code, routing_mode, failover_enabled, failure_threshold, open_duration_seconds
) VALUES
    ('00000000-0000-0000-0000-000000003003', 'BAD_CHEQUE_INQUIRY', 'PRIORITY_FAILOVER', true, 3, 30),
    ('00000000-0000-0000-0000-000000003004', 'CREDIT_RATING_INQUIRY', 'PRIORITY_FAILOVER', true, 3, 30),
    ('00000000-0000-0000-0000-000000003005', 'CIVIL_REGISTRATION_INQUIRY', 'PRIORITY_FAILOVER', true, 3, 30),
    ('00000000-0000-0000-0000-000000003006', 'MILITARY_STATUS_INQUIRY', 'PRIORITY_FAILOVER', true, 3, 30),
    ('00000000-0000-0000-0000-000000003007', 'BANK_ACCOUNT_STATUS_INQUIRY', 'PRIORITY_FAILOVER', true, 3, 30),
    ('00000000-0000-0000-0000-000000003008', 'BANKING_FACILITIES_INQUIRY', 'PRIORITY_FAILOVER', true, 3, 30),
    ('00000000-0000-0000-0000-000000003009', 'CREDIT_RATING_SUBMIT', 'PRIORITY_FAILOVER', true, 3, 30),
    ('00000000-0000-0000-0000-000000003010', 'CREDIT_RATING_POLL', 'PRIORITY_FAILOVER', false, 3, 30);
