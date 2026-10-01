CREATE TABLE inquiry_definition (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    inquiry_code VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    validity_seconds BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    requires_subject_otp BOOLEAN NOT NULL DEFAULT FALSE,
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
    response_mode VARCHAR(30) NOT NULL,
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
    CONSTRAINT ck_inquiry_response_mode CHECK (response_mode IN ('SYNCHRONOUS', 'ASYNC_CALLBACK')),
    CONSTRAINT ck_inquiry_callback_configuration CHECK (
        (response_mode = 'SYNCHRONOUS'
            AND callback_transport IS NULL
            AND callback_destination IS NULL
            AND callback_correlation_id IS NULL
            AND callback_status = 'NOT_REQUIRED')
        OR
        (response_mode = 'ASYNC_CALLBACK'
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
    id, inquiry_code, title, validity_seconds, enabled,
    requires_subject_otp, created_at, updated_at
) VALUES
    ('00000000-0000-0000-0000-000000004001', 'SHAHKAR_VERIFY', 'تطبیق مالکیت موبایل', 86400, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004002', 'CIVIL_REGISTRATION_INQUIRY', 'ثبت احوال', 86400, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004003', 'MILITARY_STATUS_INQUIRY', 'نظام وظیفه', 2592000, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004004', 'BANK_ACCOUNT_STATUS_INQUIRY', 'وضعیت حساب بانکی', 86400, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004005', 'BANKING_FACILITIES_INQUIRY', 'تسهیلات و تعهدات بانکی', 86400, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004006', 'BAD_CHEQUE_INQUIRY', 'چک برگشتی', 86400, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000004007', 'CREDIT_RATING_INQUIRY', 'رتبه اعتباری', 604800, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

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

INSERT INTO integration_provider (
    id, capability_code, provider_code, adapter_code, enabled, priority, weight,
    base_url, operation_path, authentication_type, connect_timeout_ms, read_timeout_ms, metadata_json
) VALUES
    ('00000000-0000-0000-0000-000000003201', 'CIVIL_REGISTRATION_INQUIRY', 'JETVAM_MOCK',
     'CIVIL_REGISTRATION_MOCK_V1', true, 1000, 1, 'http://localhost', '/mock/civil-registration',
     'NONE', 1000, 1000, '{"mock":true}'),
    ('00000000-0000-0000-0000-000000003202', 'MILITARY_STATUS_INQUIRY', 'JETVAM_MOCK',
     'MILITARY_STATUS_MOCK_V1', true, 1000, 1, 'http://localhost', '/mock/military-status',
     'NONE', 1000, 1000, '{"mock":true}'),
    ('00000000-0000-0000-0000-000000003203', 'BANK_ACCOUNT_STATUS_INQUIRY', 'JETVAM_MOCK',
     'BANK_ACCOUNT_STATUS_MOCK_V1', true, 1000, 1, 'http://localhost', '/mock/bank-account-status',
     'NONE', 1000, 1000, '{"mock":true}'),
    ('00000000-0000-0000-0000-000000003204', 'BANKING_FACILITIES_INQUIRY', 'JETVAM_MOCK',
     'BANKING_FACILITIES_MOCK_V1', true, 1000, 1, 'http://localhost', '/mock/banking-facilities',
     'NONE', 1000, 1000, '{"mock":true}'),
    ('00000000-0000-0000-0000-000000003205', 'BAD_CHEQUE_INQUIRY', 'JETVAM_MOCK',
     'BAD_CHEQUE_MOCK_V1', true, 1000, 1, 'http://localhost', '/mock/bad-cheque',
     'NONE', 1000, 1000, '{"mock":true}'),
    ('00000000-0000-0000-0000-000000003206', 'CREDIT_RATING_INQUIRY', 'CREDIT_BUREAU_MOCK',
     'CREDIT_RATING_MOCK_V1', true, 1000, 1, 'http://localhost', '/mock/credit-rating',
     'NONE', 1000, 1000, '{"mock":true,"protocol":"direct"}'),
    ('00000000-0000-0000-0000-000000003207', 'CREDIT_RATING_SUBMIT', 'CREDIT_BUREAU_MOCK',
     'CREDIT_RATING_SUBMIT_MOCK_V1', true, 1000, 1, 'http://localhost', '/mock/credit-rating/requests',
     'NONE', 1000, 1000, '{"mock":true,"protocol":"submit"}'),
    ('00000000-0000-0000-0000-000000003208', 'CREDIT_RATING_POLL', 'CREDIT_BUREAU_MOCK',
     'CREDIT_RATING_POLL_MOCK_V1', true, 1000, 1, 'http://localhost', '/mock/credit-rating/requests/{trackingCode}',
     'NONE', 1000, 1000, '{"mock":true,"protocol":"poll"}');
