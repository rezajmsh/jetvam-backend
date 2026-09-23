ALTER TABLE iam_individual_party ALTER COLUMN first_name DROP NOT NULL;
ALTER TABLE iam_individual_party ALTER COLUMN last_name DROP NOT NULL;
ALTER TABLE iam_individual_party ADD COLUMN shahkar_tracking_id varchar(100);

ALTER TABLE iam_user_account
    ADD COLUMN primary_authentication_method varchar(30) NOT NULL DEFAULT 'PASSWORD';
ALTER TABLE iam_user_account
    ADD CONSTRAINT ck_iam_user_auth_method
        CHECK (primary_authentication_method IN ('PASSWORD', 'OTP'));
ALTER TABLE iam_user_account
    ADD CONSTRAINT ck_iam_user_credentials
        CHECK (
            (primary_authentication_method = 'OTP' AND username IS NULL AND password_hash IS NULL)
            OR
            (primary_authentication_method = 'PASSWORD' AND username IS NOT NULL AND password_hash IS NOT NULL)
        );

CREATE UNIQUE INDEX uk_iam_user_otp_party
    ON iam_user_account(party_id)
    WHERE primary_authentication_method = 'OTP';

CREATE TABLE iam_otp_challenge (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    mobile varchar(11) NOT NULL,
    national_code varchar(10),
    purpose varchar(40) NOT NULL,
    code_digest varchar(64) NOT NULL,
    status varchar(30) NOT NULL,
    expires_at timestamptz NOT NULL,
    resend_available_at timestamptz NOT NULL,
    attempts_remaining integer NOT NULL,
    consumed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_iam_otp_purpose CHECK (purpose IN ('CUSTOMER_REGISTRATION', 'CUSTOMER_LOGIN')),
    CONSTRAINT ck_iam_otp_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'EXPIRED', 'ATTEMPTS_EXHAUSTED')),
    CONSTRAINT ck_iam_otp_attempts CHECK (attempts_remaining >= 0)
);

CREATE INDEX ix_iam_otp_mobile_purpose_created
    ON iam_otp_challenge(mobile, purpose, created_at DESC);
CREATE INDEX ix_iam_otp_expiry
    ON iam_otp_challenge(expires_at)
    WHERE status = 'ACTIVE';
