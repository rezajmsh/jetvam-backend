CREATE TABLE iam_party (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    party_type varchar(30) NOT NULL,
    status varchar(30) NOT NULL,
    display_name varchar(200) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_iam_party_type CHECK (party_type IN ('INDIVIDUAL', 'ORGANIZATION')),
    CONSTRAINT ck_iam_party_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

CREATE TABLE iam_individual_party (
    id uuid PRIMARY KEY REFERENCES iam_party(id),
    national_code varchar(10) NOT NULL UNIQUE,
    first_name varchar(100),
    last_name varchar(100),
    birth_date date,
    mobile varchar(11) UNIQUE,
    mobile_verified_at timestamptz,
    shahkar_status varchar(30) NOT NULL,
    shahkar_verified_at timestamptz,
    shahkar_tracking_id varchar(100),
    identity_verified_at timestamptz,
    CONSTRAINT ck_iam_individual_shahkar_status CHECK (
        shahkar_status IN ('NOT_REQUESTED', 'PENDING', 'MATCHED', 'NOT_MATCHED', 'FAILED')
    )
);

CREATE TABLE iam_organization_party (
    id uuid PRIMARY KEY REFERENCES iam_party(id),
    national_id varchar(20) NOT NULL UNIQUE,
    legal_name varchar(250) NOT NULL,
    registration_number varchar(50)
);

CREATE TABLE iam_role (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    code varchar(100) NOT NULL UNIQUE,
    title varchar(200) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE iam_role_permission (
    role_id uuid NOT NULL REFERENCES iam_role(id) ON DELETE CASCADE,
    permission varchar(150) NOT NULL,
    PRIMARY KEY (role_id, permission)
);

CREATE TABLE iam_user_account (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    party_id uuid NOT NULL REFERENCES iam_party(id),
    username varchar(150) UNIQUE,
    password_hash varchar(255),
    authentication_mobile varchar(11),
    primary_authentication_method varchar(30) NOT NULL,
    status varchar(30) NOT NULL,
    last_login_at timestamptz,
    failed_login_attempts integer NOT NULL DEFAULT 0,
    locked_until timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_iam_user_auth_method CHECK (primary_authentication_method IN ('PASSWORD', 'OTP')),
    CONSTRAINT ck_iam_user_credentials CHECK (
        (primary_authentication_method = 'OTP' AND username IS NULL AND password_hash IS NULL)
        OR
        (primary_authentication_method = 'PASSWORD' AND username IS NOT NULL AND password_hash IS NOT NULL)
    ),
    CONSTRAINT ck_iam_user_status CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED', 'DISABLED'))
);

CREATE TABLE iam_user_category (
    user_id uuid NOT NULL REFERENCES iam_user_account(id) ON DELETE CASCADE,
    category varchar(30) NOT NULL,
    PRIMARY KEY (user_id, category),
    CONSTRAINT ck_iam_user_category CHECK (category IN ('CUSTOMER', 'MERCHANT', 'OPERATOR', 'SERVICE'))
);

CREATE TABLE iam_user_role (
    user_id uuid NOT NULL REFERENCES iam_user_account(id) ON DELETE CASCADE,
    role_id uuid NOT NULL REFERENCES iam_role(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE iam_customer_profile (
    id uuid PRIMARY KEY REFERENCES iam_party(id),
    version bigint NOT NULL DEFAULT 0,
    onboarding_status varchar(40) NOT NULL,
    bank_card_number varchar(16),
    landline varchar(20),
    postal_code varchar(10),
    address varchar(1000),
    personal_information_revision bigint NOT NULL DEFAULT 0,
    education_code varchar(80),
    employment_code varchar(80),
    monthly_income numeric(19, 2),
    employment_information_revision bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_iam_customer_onboarding CHECK (
        onboarding_status IN (
            'MOBILE_PENDING', 'MOBILE_VERIFIED', 'IDENTITY_PENDING',
            'IDENTITY_VERIFIED', 'COMPLETED', 'REJECTED'
        )
    ),
    CONSTRAINT ck_iam_customer_personal_revision CHECK (personal_information_revision >= 0),
    CONSTRAINT ck_iam_customer_employment_revision CHECK (employment_information_revision >= 0),
    CONSTRAINT ck_iam_customer_monthly_income CHECK (monthly_income IS NULL OR monthly_income >= 0),
    CONSTRAINT ck_iam_customer_personal_complete CHECK (
        (personal_information_revision = 0
            AND bank_card_number IS NULL AND landline IS NULL AND postal_code IS NULL AND address IS NULL)
        OR
        (personal_information_revision > 0
            AND bank_card_number IS NOT NULL AND landline IS NOT NULL
            AND postal_code IS NOT NULL AND address IS NOT NULL)
    ),
    CONSTRAINT ck_iam_customer_employment_complete CHECK (
        (employment_information_revision = 0
            AND education_code IS NULL AND employment_code IS NULL AND monthly_income IS NULL)
        OR
        (employment_information_revision > 0
            AND education_code IS NOT NULL AND employment_code IS NOT NULL AND monthly_income IS NOT NULL)
    )
);

CREATE TABLE iam_customer_employment_document (
    customer_profile_id uuid NOT NULL REFERENCES iam_customer_profile(id) ON DELETE CASCADE,
    document_id uuid NOT NULL,
    PRIMARY KEY (customer_profile_id, document_id)
);

CREATE INDEX ix_iam_user_party ON iam_user_account(party_id);
CREATE INDEX ix_iam_user_status ON iam_user_account(status);
CREATE INDEX ix_iam_user_authentication_mobile
    ON iam_user_account(authentication_mobile)
    WHERE authentication_mobile IS NOT NULL;
CREATE UNIQUE INDEX uk_iam_user_otp_party
    ON iam_user_account(party_id)
    WHERE primary_authentication_method = 'OTP';

CREATE FUNCTION iam_assert_party_subtype()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    actual_type varchar(30);
BEGIN
    SELECT party_type INTO actual_type
    FROM iam_party
    WHERE id = NEW.id;

    IF actual_type IS DISTINCT FROM TG_ARGV[0] THEN
        RAISE EXCEPTION 'Party % has type %, expected %', NEW.id, actual_type, TG_ARGV[0]
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_iam_individual_party_type
    BEFORE INSERT OR UPDATE ON iam_individual_party
    FOR EACH ROW EXECUTE FUNCTION iam_assert_party_subtype('INDIVIDUAL');

CREATE TRIGGER trg_iam_organization_party_type
    BEFORE INSERT OR UPDATE ON iam_organization_party
    FOR EACH ROW EXECUTE FUNCTION iam_assert_party_subtype('ORGANIZATION');

CREATE FUNCTION iam_prevent_party_type_change()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.party_type IS DISTINCT FROM NEW.party_type THEN
        RAISE EXCEPTION 'Party type cannot be changed after creation'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_iam_party_type_immutable
    BEFORE UPDATE OF party_type ON iam_party
    FOR EACH ROW EXECUTE FUNCTION iam_prevent_party_type_change();

INSERT INTO iam_role (id, code, title) VALUES
    ('00000000-0000-0000-0000-000000000101', 'CUSTOMER', 'Customer'),
    ('00000000-0000-0000-0000-000000000102', 'MERCHANT_USER', 'Merchant user'),
    ('00000000-0000-0000-0000-000000000103', 'SYSTEM_OPERATOR', 'System operator'),
    ('00000000-0000-0000-0000-000000000104', 'UAA_ADMIN', 'Identity administrator'),
    ('00000000-0000-0000-0000-000000000105', 'SERVICE', 'Service account'),
    ('00000000-0000-0000-0000-000000000106', 'SYSTEM_ADMIN', 'System administrator'),
    ('00000000-0000-0000-0000-000000000107', 'MERCHANT_ADMIN', 'Merchant administrator'),
    ('00000000-0000-0000-0000-000000000108', 'MERCHANT_OPERATOR', 'Merchant operator');

INSERT INTO iam_role_permission (role_id, permission) VALUES
    ('00000000-0000-0000-0000-000000000101', 'loan:request:self'),
    ('00000000-0000-0000-0000-000000000101', 'profile:read:self'),
    ('00000000-0000-0000-0000-000000000101', 'profile:write:self'),
    ('00000000-0000-0000-0000-000000000101', 'product:catalog:read'),
    ('00000000-0000-0000-0000-000000000101', 'origination:self:read'),
    ('00000000-0000-0000-0000-000000000101', 'origination:self:write'),
    ('00000000-0000-0000-0000-000000000101', 'payment:self:read'),
    ('00000000-0000-0000-0000-000000000101', 'payment:self:write'),
    ('00000000-0000-0000-0000-000000000102', 'merchant:read:self'),
    ('00000000-0000-0000-0000-000000000102', 'product:catalog:read'),
    ('00000000-0000-0000-0000-000000000103', 'backoffice:access'),
    ('00000000-0000-0000-0000-000000000103', 'identity:user:read'),
    ('00000000-0000-0000-0000-000000000103', 'jobs:read'),
    ('00000000-0000-0000-0000-000000000103', 'jobs:execute'),
    ('00000000-0000-0000-0000-000000000103', 'integration:provider:read'),
    ('00000000-0000-0000-0000-000000000103', 'product:catalog:read'),
    ('00000000-0000-0000-0000-000000000103', 'product:configuration:read'),
    ('00000000-0000-0000-0000-000000000103', 'inquiry:execute'),
    ('00000000-0000-0000-0000-000000000103', 'assessment:execute'),
    ('00000000-0000-0000-0000-000000000103', 'origination:manage'),
    ('00000000-0000-0000-0000-000000000104', 'identity:user:read'),
    ('00000000-0000-0000-0000-000000000104', 'identity:user:write'),
    ('00000000-0000-0000-0000-000000000104', 'identity:client:manage'),
    ('00000000-0000-0000-0000-000000000104', 'jobs:read'),
    ('00000000-0000-0000-0000-000000000104', 'jobs:write'),
    ('00000000-0000-0000-0000-000000000104', 'jobs:execute'),
    ('00000000-0000-0000-0000-000000000104', 'integration:provider:read'),
    ('00000000-0000-0000-0000-000000000104', 'integration:provider:write'),
    ('00000000-0000-0000-0000-000000000104', 'integration:provider:override'),
    ('00000000-0000-0000-0000-000000000105', 'product:catalog:read'),
    ('00000000-0000-0000-0000-000000000105', 'inquiry:execute'),
    ('00000000-0000-0000-0000-000000000105', 'assessment:execute'),
    ('00000000-0000-0000-0000-000000000105', 'origination:process'),
    ('00000000-0000-0000-0000-000000000105', 'payment:confirm'),
    ('00000000-0000-0000-0000-000000000106', 'backoffice:access'),
    ('00000000-0000-0000-0000-000000000106', 'identity:user:read'),
    ('00000000-0000-0000-0000-000000000106', 'identity:user:write'),
    ('00000000-0000-0000-0000-000000000106', 'identity:role:manage'),
    ('00000000-0000-0000-0000-000000000106', 'identity:client:manage'),
    ('00000000-0000-0000-0000-000000000106', 'settings:read'),
    ('00000000-0000-0000-0000-000000000106', 'settings:write'),
    ('00000000-0000-0000-0000-000000000106', 'jobs:read'),
    ('00000000-0000-0000-0000-000000000106', 'jobs:write'),
    ('00000000-0000-0000-0000-000000000106', 'jobs:execute'),
    ('00000000-0000-0000-0000-000000000106', 'integration:provider:read'),
    ('00000000-0000-0000-0000-000000000106', 'integration:provider:write'),
    ('00000000-0000-0000-0000-000000000106', 'integration:provider:override'),
    ('00000000-0000-0000-0000-000000000106', 'product:catalog:read'),
    ('00000000-0000-0000-0000-000000000106', 'product:configuration:read'),
    ('00000000-0000-0000-0000-000000000106', 'product:configuration:write'),
    ('00000000-0000-0000-0000-000000000106', 'inquiry:execute'),
    ('00000000-0000-0000-0000-000000000106', 'assessment:execute'),
    ('00000000-0000-0000-0000-000000000106', 'origination:manage'),
    ('00000000-0000-0000-0000-000000000107', 'merchant:read:self'),
    ('00000000-0000-0000-0000-000000000107', 'merchant:write:self'),
    ('00000000-0000-0000-0000-000000000107', 'merchant:user:read:self'),
    ('00000000-0000-0000-0000-000000000107', 'merchant:user:write:self'),
    ('00000000-0000-0000-0000-000000000107', 'product:catalog:read'),
    ('00000000-0000-0000-0000-000000000108', 'merchant:read:self'),
    ('00000000-0000-0000-0000-000000000108', 'product:catalog:read');
