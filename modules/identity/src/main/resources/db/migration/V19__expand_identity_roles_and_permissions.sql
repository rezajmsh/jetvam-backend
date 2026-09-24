INSERT INTO iam_role (id, code, title) VALUES
    ('00000000-0000-0000-0000-000000000106', 'SYSTEM_ADMIN', 'System administrator'),
    ('00000000-0000-0000-0000-000000000107', 'MERCHANT_ADMIN', 'Merchant administrator'),
    ('00000000-0000-0000-0000-000000000108', 'MERCHANT_OPERATOR', 'Merchant operator');

INSERT INTO iam_role_permission (role_id, permission) VALUES
    ('00000000-0000-0000-0000-000000000101', 'profile:write:self'),
    ('00000000-0000-0000-0000-000000000103', 'identity:user:read'),
    ('00000000-0000-0000-0000-000000000103', 'jobs:read'),
    ('00000000-0000-0000-0000-000000000103', 'jobs:execute'),
    ('00000000-0000-0000-0000-000000000104', 'jobs:read'),
    ('00000000-0000-0000-0000-000000000104', 'jobs:write'),
    ('00000000-0000-0000-0000-000000000104', 'jobs:execute'),
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
    ('00000000-0000-0000-0000-000000000107', 'merchant:read:self'),
    ('00000000-0000-0000-0000-000000000107', 'merchant:write:self'),
    ('00000000-0000-0000-0000-000000000107', 'merchant:user:read:self'),
    ('00000000-0000-0000-0000-000000000107', 'merchant:user:write:self'),
    ('00000000-0000-0000-0000-000000000108', 'merchant:read:self');

INSERT INTO iam_role_permission (role_id, permission)
VALUES ('00000000-0000-0000-0000-000000000102', 'merchant:read:self')
ON CONFLICT DO NOTHING;
