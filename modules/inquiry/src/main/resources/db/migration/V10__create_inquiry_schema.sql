CREATE TABLE inquiry_definition (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    inquiry_code VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    validity_seconds BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_inquiry_definition_validity CHECK (validity_seconds >= 0)
);

CREATE TABLE inquiry_request (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    inquiry_code VARCHAR(100) NOT NULL,
    national_code VARCHAR(10) NOT NULL,
    subject_key VARCHAR(150) NOT NULL,
    request_json TEXT NOT NULL,
    execution_mode VARCHAR(20) NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider_code VARCHAR(100),
    external_tracking_code VARCHAR(150),
    result_json TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    valid_until TIMESTAMP WITH TIME ZONE,
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    reused_from_request_id UUID REFERENCES inquiry_request(id),
    rejection_code VARCHAR(100),
    result_message VARCHAR(1000),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    processing_started_at TIMESTAMP WITH TIME ZONE,
    callback_transport VARCHAR(50),
    callback_destination VARCHAR(150),
    callback_correlation_id VARCHAR(150),
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
    CONSTRAINT fk_inquiry_request_definition FOREIGN KEY (inquiry_code)
        REFERENCES inquiry_definition(inquiry_code),
    CONSTRAINT ck_inquiry_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_inquiry_callback_attempt_count CHECK (callback_attempt_count >= 0),
    CONSTRAINT ck_inquiry_execution_mode CHECK (execution_mode IN ('SYNCHRONOUS', 'ASYNCHRONOUS')),
    CONSTRAINT ck_inquiry_callback_configuration CHECK (
        (execution_mode = 'SYNCHRONOUS'
            AND callback_transport IS NULL
            AND callback_destination IS NULL
            AND callback_correlation_id IS NULL
            AND callback_status = 'NOT_REQUIRED')
        OR
        (execution_mode = 'ASYNCHRONOUS'
            AND callback_transport IS NOT NULL
            AND callback_destination IS NOT NULL
            AND callback_correlation_id IS NOT NULL
            AND callback_status <> 'NOT_REQUIRED')
    ),
    CONSTRAINT ck_inquiry_cache_source CHECK (
        (cache_hit = FALSE AND reused_from_request_id IS NULL)
        OR (cache_hit = TRUE AND reused_from_request_id IS NOT NULL)
    )
);

CREATE INDEX ix_inquiry_request_due ON inquiry_request(status, next_attempt_at);
CREATE INDEX ix_inquiry_callback_due ON inquiry_request(callback_status, callback_next_attempt_at);
CREATE INDEX ix_inquiry_request_valid_result
    ON inquiry_request(inquiry_code, subject_key, valid_until DESC)
    WHERE status = 'COMPLETED';

INSERT INTO inquiry_definition (
    id, inquiry_code, title, validity_seconds, enabled, created_at, updated_at
) VALUES
    ('00000000-0000-0000-0000-000000004001', 'SHAHKAR_VERIFY', 'Mobile ownership', 86400, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004002', 'CIVIL_REGISTRATION_INQUIRY', 'Civil registration', 86400, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004003', 'MILITARY_STATUS_INQUIRY', 'Military status', 2592000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004004', 'BANK_ACCOUNT_STATUS_INQUIRY', 'Bank account status', 86400, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004005', 'BANKING_FACILITIES_INQUIRY', 'Banking facilities', 86400, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004006', 'BAD_CHEQUE_INQUIRY', 'Bad cheque', 86400, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004007', 'CREDIT_RATING_INQUIRY', 'Credit rating', 604800, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

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
