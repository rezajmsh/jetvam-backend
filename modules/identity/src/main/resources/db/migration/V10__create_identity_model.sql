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
    version bigint NOT NULL DEFAULT 0,
    national_code varchar(10) NOT NULL UNIQUE,
    first_name varchar(100) NOT NULL,
    last_name varchar(100) NOT NULL,
    birth_date date,
    mobile varchar(11) UNIQUE,
    mobile_verified_at timestamptz,
    shahkar_status varchar(30) NOT NULL,
    shahkar_verified_at timestamptz,
    identity_verified_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_iam_individual_shahkar_status CHECK (
        shahkar_status IN ('NOT_REQUESTED', 'PENDING', 'MATCHED', 'NOT_MATCHED', 'FAILED')
    )
);

CREATE TABLE iam_organization_party (
    id uuid PRIMARY KEY REFERENCES iam_party(id),
    version bigint NOT NULL DEFAULT 0,
    national_id varchar(20) NOT NULL UNIQUE,
    legal_name varchar(250) NOT NULL,
    registration_number varchar(50),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    status varchar(30) NOT NULL,
    last_login_at timestamptz,
    failed_login_attempts integer NOT NULL DEFAULT 0,
    locked_until timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_iam_customer_onboarding CHECK (
        onboarding_status IN (
            'MOBILE_PENDING', 'MOBILE_VERIFIED', 'IDENTITY_PENDING',
            'IDENTITY_VERIFIED', 'COMPLETED', 'REJECTED'
        )
    )
);

CREATE INDEX ix_iam_user_party ON iam_user_account(party_id);
CREATE INDEX ix_iam_user_status ON iam_user_account(status);

INSERT INTO iam_role (id, code, title) VALUES
    ('00000000-0000-0000-0000-000000000101', 'CUSTOMER', 'Customer'),
    ('00000000-0000-0000-0000-000000000102', 'MERCHANT_USER', 'Merchant user'),
    ('00000000-0000-0000-0000-000000000103', 'SYSTEM_OPERATOR', 'System operator'),
    ('00000000-0000-0000-0000-000000000104', 'UAA_ADMIN', 'Identity administrator'),
    ('00000000-0000-0000-0000-000000000105', 'SERVICE', 'Service account');

INSERT INTO iam_role_permission (role_id, permission) VALUES
    ('00000000-0000-0000-0000-000000000101', 'loan:request:self'),
    ('00000000-0000-0000-0000-000000000101', 'profile:read:self'),
    ('00000000-0000-0000-0000-000000000102', 'merchant:read:self'),
    ('00000000-0000-0000-0000-000000000103', 'backoffice:access'),
    ('00000000-0000-0000-0000-000000000104', 'identity:user:read'),
    ('00000000-0000-0000-0000-000000000104', 'identity:user:write'),
    ('00000000-0000-0000-0000-000000000104', 'identity:client:manage');
