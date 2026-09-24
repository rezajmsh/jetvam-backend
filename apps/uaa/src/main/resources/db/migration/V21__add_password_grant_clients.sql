-- Interactive password sessions are intentionally disabled so a configured second factor
-- cannot be bypassed. Browser clients use the explicit password grant below.
DELETE FROM uaa_oauth_client_grant_type
WHERE oauth_client_id IN (
    '00000000-0000-0000-0000-000000001001',
    '00000000-0000-0000-0000-000000001002'
)
AND grant_type = 'authorization_code';

INSERT INTO uaa_oauth_client_grant_type (oauth_client_id, grant_type) VALUES
    ('00000000-0000-0000-0000-000000001002', 'urn:jetvam:params:oauth:grant-type:password');

INSERT INTO uaa_oauth_client (
    id, client_id, client_name, require_proof_key, require_consent,
    access_token_ttl_seconds, refresh_token_ttl_seconds, reuse_refresh_tokens
) VALUES
    ('00000000-0000-0000-0000-000000001003', 'jetvam-merchant-portal',
     'Jetvam Merchant Portal', true, false, 900, 2592000, false);

INSERT INTO uaa_oauth_client_auth_method (oauth_client_id, authentication_method) VALUES
    ('00000000-0000-0000-0000-000000001003', 'none');

INSERT INTO uaa_oauth_client_grant_type (oauth_client_id, grant_type) VALUES
    ('00000000-0000-0000-0000-000000001003', 'urn:jetvam:params:oauth:grant-type:password'),
    ('00000000-0000-0000-0000-000000001003', 'refresh_token');

INSERT INTO uaa_oauth_client_scope (oauth_client_id, scope) VALUES
    ('00000000-0000-0000-0000-000000001003', 'jetvam.api'),
    ('00000000-0000-0000-0000-000000001003', 'offline_access');
