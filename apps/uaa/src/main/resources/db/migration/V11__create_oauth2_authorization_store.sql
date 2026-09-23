CREATE TABLE uaa_oauth_client (
    id varchar(100) PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    client_id varchar(100) NOT NULL UNIQUE,
    client_id_issued_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_secret varchar(200),
    client_secret_expires_at timestamptz,
    client_name varchar(200) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    require_proof_key boolean NOT NULL DEFAULT true,
    require_consent boolean NOT NULL DEFAULT false,
    access_token_ttl_seconds bigint NOT NULL DEFAULT 900,
    refresh_token_ttl_seconds bigint NOT NULL DEFAULT 2592000,
    reuse_refresh_tokens boolean NOT NULL DEFAULT false
);

CREATE TABLE uaa_oauth_client_auth_method (
    oauth_client_id varchar(100) NOT NULL REFERENCES uaa_oauth_client(id) ON DELETE CASCADE,
    authentication_method varchar(100) NOT NULL,
    PRIMARY KEY (oauth_client_id, authentication_method)
);

CREATE TABLE uaa_oauth_client_grant_type (
    oauth_client_id varchar(100) NOT NULL REFERENCES uaa_oauth_client(id) ON DELETE CASCADE,
    grant_type varchar(100) NOT NULL,
    PRIMARY KEY (oauth_client_id, grant_type)
);

CREATE TABLE uaa_oauth_client_redirect_uri (
    oauth_client_id varchar(100) NOT NULL REFERENCES uaa_oauth_client(id) ON DELETE CASCADE,
    redirect_uri varchar(1000) NOT NULL,
    PRIMARY KEY (oauth_client_id, redirect_uri)
);

CREATE TABLE uaa_oauth_client_post_logout_uri (
    oauth_client_id varchar(100) NOT NULL REFERENCES uaa_oauth_client(id) ON DELETE CASCADE,
    post_logout_redirect_uri varchar(1000) NOT NULL,
    PRIMARY KEY (oauth_client_id, post_logout_redirect_uri)
);

CREATE TABLE uaa_oauth_client_scope (
    oauth_client_id varchar(100) NOT NULL REFERENCES uaa_oauth_client(id) ON DELETE CASCADE,
    scope varchar(200) NOT NULL,
    PRIMARY KEY (oauth_client_id, scope)
);

CREATE TABLE oauth2_authorization (
    id varchar(100) PRIMARY KEY,
    registered_client_id varchar(100) NOT NULL REFERENCES uaa_oauth_client(id),
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000),
    attributes text,
    state varchar(500),
    authorization_code_value text,
    authorization_code_issued_at timestamptz,
    authorization_code_expires_at timestamptz,
    authorization_code_metadata text,
    access_token_value text,
    access_token_issued_at timestamptz,
    access_token_expires_at timestamptz,
    access_token_metadata text,
    access_token_type varchar(100),
    access_token_scopes varchar(1000),
    oidc_id_token_value text,
    oidc_id_token_issued_at timestamptz,
    oidc_id_token_expires_at timestamptz,
    oidc_id_token_metadata text,
    refresh_token_value text,
    refresh_token_issued_at timestamptz,
    refresh_token_expires_at timestamptz,
    refresh_token_metadata text,
    user_code_value text,
    user_code_issued_at timestamptz,
    user_code_expires_at timestamptz,
    user_code_metadata text,
    device_code_value text,
    device_code_issued_at timestamptz,
    device_code_expires_at timestamptz,
    device_code_metadata text
);

CREATE TABLE oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL REFERENCES uaa_oauth_client(id),
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

CREATE INDEX ix_oauth2_authorization_principal ON oauth2_authorization(principal_name);
CREATE INDEX ix_oauth2_authorization_state ON oauth2_authorization(state);

INSERT INTO uaa_oauth_client (
    id, client_id, client_name, require_proof_key, require_consent,
    access_token_ttl_seconds, refresh_token_ttl_seconds, reuse_refresh_tokens
) VALUES
    ('00000000-0000-0000-0000-000000001001', 'jetvam-portal', 'Jetvam Customer Portal', true, false, 900, 2592000, false),
    ('00000000-0000-0000-0000-000000001002', 'jetvam-backoffice', 'Jetvam Backoffice', true, false, 900, 2592000, false);

INSERT INTO uaa_oauth_client_auth_method (oauth_client_id, authentication_method) VALUES
    ('00000000-0000-0000-0000-000000001001', 'none'),
    ('00000000-0000-0000-0000-000000001002', 'none');

INSERT INTO uaa_oauth_client_grant_type (oauth_client_id, grant_type) VALUES
    ('00000000-0000-0000-0000-000000001001', 'authorization_code'),
    ('00000000-0000-0000-0000-000000001001', 'refresh_token'),
    ('00000000-0000-0000-0000-000000001002', 'authorization_code'),
    ('00000000-0000-0000-0000-000000001002', 'refresh_token');

INSERT INTO uaa_oauth_client_redirect_uri (oauth_client_id, redirect_uri) VALUES
    ('00000000-0000-0000-0000-000000001001', 'http://127.0.0.1:3000/oauth/callback'),
    ('00000000-0000-0000-0000-000000001002', 'http://127.0.0.1:3001/oauth/callback');

INSERT INTO uaa_oauth_client_post_logout_uri (oauth_client_id, post_logout_redirect_uri) VALUES
    ('00000000-0000-0000-0000-000000001001', 'http://127.0.0.1:3000/'),
    ('00000000-0000-0000-0000-000000001002', 'http://127.0.0.1:3001/');

INSERT INTO uaa_oauth_client_scope (oauth_client_id, scope) VALUES
    ('00000000-0000-0000-0000-000000001001', 'openid'),
    ('00000000-0000-0000-0000-000000001001', 'profile'),
    ('00000000-0000-0000-0000-000000001001', 'offline_access'),
    ('00000000-0000-0000-0000-000000001001', 'jetvam.api'),
    ('00000000-0000-0000-0000-000000001002', 'openid'),
    ('00000000-0000-0000-0000-000000001002', 'profile'),
    ('00000000-0000-0000-0000-000000001002', 'offline_access'),
    ('00000000-0000-0000-0000-000000001002', 'jetvam.api');
