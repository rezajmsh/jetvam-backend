CREATE TABLE integration_provider_route (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    capability_code varchar(100) NOT NULL UNIQUE,
    routing_mode varchar(30) NOT NULL,
    failover_enabled boolean NOT NULL,
    forced_provider_code varchar(100),
    forced_until timestamptz,
    failure_threshold integer NOT NULL,
    open_duration_seconds bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_integration_route_mode CHECK (
        routing_mode IN ('PRIORITY_FAILOVER', 'ROUND_ROBIN', 'WEIGHTED', 'MANUAL_ONLY')
    ),
    CONSTRAINT ck_integration_route_failure_threshold CHECK (failure_threshold > 0),
    CONSTRAINT ck_integration_route_open_duration CHECK (open_duration_seconds > 0),
    CONSTRAINT ck_integration_route_override CHECK (
        (forced_provider_code IS NULL AND forced_until IS NULL)
        OR (forced_provider_code IS NOT NULL AND forced_until IS NOT NULL)
    )
);

CREATE TABLE integration_tls_profile (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    profile_code varchar(100) NOT NULL UNIQUE,
    store_type varchar(30) NOT NULL,
    trust_store_location varchar(1000),
    trust_store_password_ref varchar(1000),
    key_store_location varchar(1000),
    key_store_password_ref varchar(1000),
    key_password_ref varchar(1000),
    enabled_protocols varchar(200) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_integration_tls_key_store CHECK (
        key_store_location IS NULL OR key_store_password_ref IS NOT NULL
    )
);

CREATE TABLE integration_provider (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    capability_code varchar(100) NOT NULL,
    provider_code varchar(100) NOT NULL,
    adapter_code varchar(120) NOT NULL,
    enabled boolean NOT NULL,
    priority integer NOT NULL,
    weight integer NOT NULL,
    base_url varchar(1000) NOT NULL,
    operation_path varchar(500) NOT NULL,
    authentication_type varchar(30) NOT NULL,
    authentication_header varchar(150),
    authentication_username varchar(250),
    credential_secret_ref varchar(1000),
    tls_profile_code varchar(100),
    connect_timeout_ms bigint NOT NULL,
    read_timeout_ms bigint NOT NULL,
    metadata_json text NOT NULL DEFAULT '{}',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_integration_provider_capability_code UNIQUE (capability_code, provider_code),
    CONSTRAINT fk_integration_provider_route FOREIGN KEY (capability_code)
        REFERENCES integration_provider_route(capability_code),
    CONSTRAINT fk_integration_provider_tls FOREIGN KEY (tls_profile_code)
        REFERENCES integration_tls_profile(profile_code),
    CONSTRAINT ck_integration_provider_auth_type CHECK (
        authentication_type IN ('NONE', 'API_KEY', 'BASIC', 'BEARER', 'CUSTOM')
    ),
    CONSTRAINT ck_integration_provider_priority CHECK (priority >= 0),
    CONSTRAINT ck_integration_provider_weight CHECK (weight > 0),
    CONSTRAINT ck_integration_provider_timeouts CHECK (connect_timeout_ms > 0 AND read_timeout_ms > 0)
);

CREATE INDEX ix_integration_provider_routing
    ON integration_provider(capability_code, enabled, priority, provider_code);

INSERT INTO integration_provider_route (
    id, capability_code, routing_mode, failover_enabled, failure_threshold, open_duration_seconds
) VALUES
    ('00000000-0000-0000-0000-000000003001', 'SHAHKAR_VERIFY', 'PRIORITY_FAILOVER', true, 3, 30),
    ('00000000-0000-0000-0000-000000003002', 'SMS_SEND', 'PRIORITY_FAILOVER', true, 3, 30);
