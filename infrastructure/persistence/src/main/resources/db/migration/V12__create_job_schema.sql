create table job_definition
(
    id              uuid primary key,
    version         bigint                   not null default 0,
    code            varchar(100)             not null,
    display_name    varchar(200)             not null,
    description     varchar(1000),
    handler_key     varchar(150)             not null,
    cron_expression varchar(120)             not null,
    time_zone       varchar(80)              not null,
    enabled         boolean                  not null default true,
    lock_owner      varchar(200),
    lock_until      timestamp with time zone,
    created_at      timestamp with time zone not null default current_timestamp,
    updated_at      timestamp with time zone not null default current_timestamp,
    constraint uk_job_definition_code unique (code),
    constraint ck_job_definition_code_not_blank check (btrim(code) <> ''),
    constraint ck_job_definition_handler_not_blank check (btrim(handler_key) <> ''),
    constraint ck_job_definition_cron_not_blank check (btrim(cron_expression) <> '')
);

create table job_execution
(
    id                uuid primary key,
    version           bigint                   not null default 0,
    job_definition_id uuid                     not null references job_definition (id),
    job_code          varchar(100)             not null,
    handler_key       varchar(150)             not null,
    trigger_type      varchar(20)              not null,
    status            varchar(20)              not null,
    requested_by      varchar(200),
    scheduled_at      timestamp with time zone,
    started_at        timestamp with time zone,
    finished_at       timestamp with time zone,
    duration_ms       bigint,
    processed_count   bigint                   not null default 0,
    succeeded_count   bigint                   not null default 0,
    failed_count      bigint                   not null default 0,
    result_summary    text,
    error_type        varchar(250),
    error_message     text,
    error_stack_trace text,
    created_at        timestamp with time zone not null default current_timestamp,
    updated_at        timestamp with time zone not null default current_timestamp,
    constraint ck_job_execution_trigger check (trigger_type in ('SCHEDULED', 'MANUAL')),
    constraint ck_job_execution_status check (status in ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    constraint ck_job_execution_duration check (duration_ms is null or duration_ms >= 0),
    constraint ck_job_execution_counts check (
        processed_count >= 0 and succeeded_count >= 0 and failed_count >= 0
        and processed_count = succeeded_count + failed_count
    )
);

create index ix_job_execution_definition_created
    on job_execution (job_definition_id, created_at desc);
create index ix_job_execution_status_created
    on job_execution (status, created_at desc);

create index ix_job_definition_lock
    on job_definition (lock_until) where lock_until is not null;

create table job_execution_item
(
    id                 uuid primary key,
    version            bigint                   not null default 0,
    job_execution_id   uuid                     not null references job_execution (id),
    sequence_number    integer                  not null,
    item_type          varchar(50)              not null,
    item_key           varchar(150)             not null,
    operation_code     varchar(100),
    subject_identifier varchar(150),
    subject_key        varchar(150),
    status             varchar(20)              not null,
    business_status    varchar(80),
    provider_code      varchar(100),
    external_reference varchar(150),
    message            varchar(1000),
    created_at         timestamp with time zone not null default current_timestamp,
    updated_at         timestamp with time zone not null default current_timestamp,
    constraint uk_job_execution_item_sequence unique (job_execution_id, sequence_number),
    constraint ck_job_execution_item_status check (status in ('SUCCEEDED', 'FAILED')),
    constraint ck_job_execution_item_sequence check (sequence_number > 0)
);

create index ix_job_execution_item_execution
    on job_execution_item (job_execution_id, sequence_number);

create index ix_job_execution_item_subject
    on job_execution_item (subject_identifier, operation_code);

comment on table job_definition is
    'Application-owned background job catalog and distributed execution lease';
comment on table job_execution is 'Unified scheduled and manual job execution history';
comment on table job_execution_item is
    'Immutable item-level audit trail for every managed job execution';

insert into job_definition
    (id, version, code, display_name, description, handler_key, cron_expression, time_zone, enabled)
values
    ('b981498a-122b-48a1-851d-338259c0f2a1', 0, 'notification-dispatch',
     'Notification outbox dispatcher',
     'Claims due notification outbox records and delegates delivery to the notification module',
     'notification-dispatch', '0/5 * * * * ?', 'Asia/Tehran', true),
    ('b981498a-122b-48a1-851d-338259c0f2a2', 0, 'inquiry-execution',
     'Inquiry provider execution',
     'Claims queued inquiry requests and executes provider submission or polling',
     'inquiry-execution', '0/15 * * * * ?', 'Asia/Tehran', true),
    ('b981498a-122b-48a1-851d-338259c0f2a3', 0, 'inquiry-callback',
     'Inquiry callback delivery',
     'Delivers completed inquiry results to their configured workflow consumers',
     'inquiry-callback', '0/5 * * * * ?', 'Asia/Tehran', true);
