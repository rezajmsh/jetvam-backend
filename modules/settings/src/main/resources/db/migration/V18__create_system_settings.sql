CREATE TABLE system_setting (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    setting_key varchar(150) NOT NULL UNIQUE,
    setting_value varchar(2000) NOT NULL,
    value_type varchar(20) NOT NULL,
    description varchar(500) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_system_setting_value_type CHECK (value_type IN ('BOOLEAN', 'INTEGER', 'TEXT'))
);

INSERT INTO system_setting (id, setting_key, setting_value, value_type, description) VALUES
    ('00000000-0000-0000-0000-000000002001',
     'security.system-users.two-factor-required', 'false', 'BOOLEAN',
     'Requires a mobile OTP in addition to the password for system administrators and operators.'),
    ('00000000-0000-0000-0000-000000002002',
     'security.merchant-users.two-factor-required', 'false', 'BOOLEAN',
     'Requires a mobile OTP in addition to the password for merchant administrators and operators.');
