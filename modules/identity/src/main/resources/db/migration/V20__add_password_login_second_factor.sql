ALTER TABLE iam_otp_challenge DROP CONSTRAINT ck_iam_otp_purpose;
ALTER TABLE iam_otp_challenge ADD CONSTRAINT ck_iam_otp_purpose CHECK (
    purpose IN ('CUSTOMER_REGISTRATION', 'CUSTOMER_LOGIN', 'PASSWORD_LOGIN_SECOND_FACTOR')
);
