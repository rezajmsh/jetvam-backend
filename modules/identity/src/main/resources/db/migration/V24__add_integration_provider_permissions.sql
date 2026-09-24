INSERT INTO iam_role_permission (role_id, permission) VALUES
    ('00000000-0000-0000-0000-000000000103', 'integration:provider:read'),
    ('00000000-0000-0000-0000-000000000104', 'integration:provider:read'),
    ('00000000-0000-0000-0000-000000000104', 'integration:provider:write'),
    ('00000000-0000-0000-0000-000000000104', 'integration:provider:override'),
    ('00000000-0000-0000-0000-000000000106', 'integration:provider:read'),
    ('00000000-0000-0000-0000-000000000106', 'integration:provider:write'),
    ('00000000-0000-0000-0000-000000000106', 'integration:provider:override')
ON CONFLICT DO NOTHING;
