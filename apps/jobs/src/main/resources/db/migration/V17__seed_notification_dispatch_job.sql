insert into job_definition
    (id, version, code, display_name, description, handler_key, cron_expression, time_zone, enabled)
values
    ('b981498a-122b-48a1-851d-338259c0f2a1', 0, 'notification-dispatch',
     'Notification outbox dispatcher',
     'Claims due notification outbox records and delegates delivery to the notification module',
     'notification-dispatch', '0/5 * * * * ?', 'Asia/Tehran', true)
on conflict (code) do nothing;
