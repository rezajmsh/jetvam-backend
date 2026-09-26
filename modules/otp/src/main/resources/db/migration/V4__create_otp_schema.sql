CREATE TABLE otp_challenge (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    mobile varchar(11) NOT NULL,
    national_code varchar(10),
    purpose varchar(80) NOT NULL,
    code_digest varchar(64) NOT NULL,
    status varchar(30) NOT NULL,
    expires_at timestamptz NOT NULL,
    resend_available_at timestamptz NOT NULL,
    attempts_remaining integer NOT NULL,
    consumed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_otp_purpose CHECK (length(trim(purpose)) > 0),
    CONSTRAINT ck_otp_status CHECK (
        status IN ('ACTIVE', 'CONSUMED', 'EXPIRED', 'ATTEMPTS_EXHAUSTED')
    ),
    CONSTRAINT ck_otp_attempts CHECK (attempts_remaining >= 0)
);

CREATE INDEX ix_otp_mobile_purpose_created
    ON otp_challenge(mobile, purpose, created_at DESC);
CREATE INDEX ix_otp_expiry
    ON otp_challenge(expires_at)
    WHERE status = 'ACTIVE';
