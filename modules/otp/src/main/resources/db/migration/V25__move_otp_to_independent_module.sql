DO $$
BEGIN
    IF to_regclass(current_schema() || '.otp_challenge') IS NULL THEN
        IF to_regclass(current_schema() || '.iam_otp_challenge') IS NOT NULL THEN
            ALTER TABLE iam_otp_challenge RENAME TO otp_challenge;
        ELSE
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
                CONSTRAINT ck_otp_status CHECK (
                    status IN ('ACTIVE', 'CONSUMED', 'EXPIRED', 'ATTEMPTS_EXHAUSTED')
                ),
                CONSTRAINT ck_otp_attempts CHECK (attempts_remaining >= 0)
            );
        END IF;
    END IF;

    IF to_regclass(current_schema() || '.ix_iam_otp_mobile_purpose_created') IS NOT NULL
            AND to_regclass(current_schema() || '.ix_otp_mobile_purpose_created') IS NULL THEN
        ALTER INDEX ix_iam_otp_mobile_purpose_created RENAME TO ix_otp_mobile_purpose_created;
    END IF;
    IF to_regclass(current_schema() || '.ix_iam_otp_expiry') IS NOT NULL
            AND to_regclass(current_schema() || '.ix_otp_expiry') IS NULL THEN
        ALTER INDEX ix_iam_otp_expiry RENAME TO ix_otp_expiry;
    END IF;
END $$;

ALTER TABLE otp_challenge DROP CONSTRAINT IF EXISTS ck_iam_otp_purpose;
ALTER TABLE otp_challenge DROP CONSTRAINT IF EXISTS ck_otp_purpose;
ALTER TABLE otp_challenge ALTER COLUMN purpose TYPE varchar(80);
ALTER TABLE otp_challenge ADD CONSTRAINT ck_otp_purpose CHECK (length(trim(purpose)) > 0);

CREATE INDEX IF NOT EXISTS ix_otp_mobile_purpose_created
    ON otp_challenge(mobile, purpose, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_otp_expiry
    ON otp_challenge(expires_at)
    WHERE status = 'ACTIVE';
