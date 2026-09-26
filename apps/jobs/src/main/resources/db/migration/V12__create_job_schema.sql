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

create table qrtz_job_details
(
    sched_name varchar(120) not null,
    job_name varchar(200) not null,
    job_group varchar(200) not null,
    description varchar(250),
    job_class_name varchar(250) not null,
    is_durable boolean not null,
    is_nonconcurrent boolean not null,
    is_update_data boolean not null,
    requests_recovery boolean not null,
    job_data bytea,
    primary key (sched_name, job_name, job_group)
);

create table qrtz_triggers
(
    sched_name varchar(120) not null,
    trigger_name varchar(200) not null,
    trigger_group varchar(200) not null,
    job_name varchar(200) not null,
    job_group varchar(200) not null,
    description varchar(250),
    next_fire_time bigint,
    prev_fire_time bigint,
    priority integer,
    trigger_state varchar(16) not null,
    trigger_type varchar(8) not null,
    start_time bigint not null,
    end_time bigint,
    calendar_name varchar(200),
    misfire_instr smallint,
    job_data bytea,
    primary key (sched_name, trigger_name, trigger_group),
    foreign key (sched_name, job_name, job_group)
        references qrtz_job_details (sched_name, job_name, job_group)
);

create table qrtz_simple_triggers
(
    sched_name varchar(120) not null,
    trigger_name varchar(200) not null,
    trigger_group varchar(200) not null,
    repeat_count bigint not null,
    repeat_interval bigint not null,
    times_triggered bigint not null,
    primary key (sched_name, trigger_name, trigger_group),
    foreign key (sched_name, trigger_name, trigger_group)
        references qrtz_triggers (sched_name, trigger_name, trigger_group)
);

create table qrtz_cron_triggers
(
    sched_name varchar(120) not null,
    trigger_name varchar(200) not null,
    trigger_group varchar(200) not null,
    cron_expression varchar(120) not null,
    time_zone_id varchar(80),
    primary key (sched_name, trigger_name, trigger_group),
    foreign key (sched_name, trigger_name, trigger_group)
        references qrtz_triggers (sched_name, trigger_name, trigger_group)
);

create table qrtz_simprop_triggers
(
    sched_name varchar(120) not null,
    trigger_name varchar(200) not null,
    trigger_group varchar(200) not null,
    str_prop_1 varchar(512), str_prop_2 varchar(512), str_prop_3 varchar(512),
    int_prop_1 integer, int_prop_2 integer,
    long_prop_1 bigint, long_prop_2 bigint,
    dec_prop_1 numeric(13, 4), dec_prop_2 numeric(13, 4),
    bool_prop_1 boolean, bool_prop_2 boolean,
    primary key (sched_name, trigger_name, trigger_group),
    foreign key (sched_name, trigger_name, trigger_group)
        references qrtz_triggers (sched_name, trigger_name, trigger_group)
);

create table qrtz_blob_triggers
(
    sched_name varchar(120) not null,
    trigger_name varchar(200) not null,
    trigger_group varchar(200) not null,
    blob_data bytea,
    primary key (sched_name, trigger_name, trigger_group),
    foreign key (sched_name, trigger_name, trigger_group)
        references qrtz_triggers (sched_name, trigger_name, trigger_group)
);

create table qrtz_calendars
(
    sched_name varchar(120) not null,
    calendar_name varchar(200) not null,
    calendar bytea not null,
    primary key (sched_name, calendar_name)
);

create table qrtz_paused_trigger_grps
(
    sched_name varchar(120) not null,
    trigger_group varchar(200) not null,
    primary key (sched_name, trigger_group)
);

create table qrtz_fired_triggers
(
    sched_name varchar(120) not null,
    entry_id varchar(95) not null,
    trigger_name varchar(200) not null,
    trigger_group varchar(200) not null,
    instance_name varchar(200) not null,
    fired_time bigint not null,
    sched_time bigint not null,
    priority integer not null,
    state varchar(16) not null,
    job_name varchar(200),
    job_group varchar(200),
    is_nonconcurrent boolean,
    requests_recovery boolean,
    primary key (sched_name, entry_id)
);

create table qrtz_scheduler_state
(
    sched_name varchar(120) not null,
    instance_name varchar(200) not null,
    last_checkin_time bigint not null,
    checkin_interval bigint not null,
    primary key (sched_name, instance_name)
);

create table qrtz_locks
(
    sched_name varchar(120) not null,
    lock_name varchar(40) not null,
    primary key (sched_name, lock_name)
);

create index idx_qrtz_j_req_recovery on qrtz_job_details (sched_name, requests_recovery);
create index idx_qrtz_j_grp on qrtz_job_details (sched_name, job_group);
create index idx_qrtz_t_j on qrtz_triggers (sched_name, job_name, job_group);
create index idx_qrtz_t_jg on qrtz_triggers (sched_name, job_group);
create index idx_qrtz_t_c on qrtz_triggers (sched_name, calendar_name);
create index idx_qrtz_t_g on qrtz_triggers (sched_name, trigger_group);
create index idx_qrtz_t_state on qrtz_triggers (sched_name, trigger_state);
create index idx_qrtz_t_n_state on qrtz_triggers (sched_name, trigger_name, trigger_group, trigger_state);
create index idx_qrtz_t_n_g_state on qrtz_triggers (sched_name, trigger_group, trigger_state);
create index idx_qrtz_t_next_fire_time on qrtz_triggers (sched_name, next_fire_time);
create index idx_qrtz_t_nft_st on qrtz_triggers (sched_name, trigger_state, next_fire_time);
create index idx_qrtz_t_nft_misfire on qrtz_triggers (sched_name, misfire_instr, next_fire_time);
create index idx_qrtz_t_nft_st_misfire on qrtz_triggers (sched_name, misfire_instr, next_fire_time, trigger_state);
create index idx_qrtz_t_nft_st_misfire_grp
    on qrtz_triggers (sched_name, misfire_instr, next_fire_time, trigger_group, trigger_state);
create index idx_qrtz_ft_trig_inst_name on qrtz_fired_triggers (sched_name, instance_name);
create index idx_qrtz_ft_inst_job_req_rcvry
    on qrtz_fired_triggers (sched_name, instance_name, requests_recovery);
create index idx_qrtz_ft_j_g on qrtz_fired_triggers (sched_name, job_name, job_group);
create index idx_qrtz_ft_jg on qrtz_fired_triggers (sched_name, job_group);
create index idx_qrtz_ft_t_g on qrtz_fired_triggers (sched_name, trigger_name, trigger_group);
create index idx_qrtz_ft_tg on qrtz_fired_triggers (sched_name, trigger_group);

comment on table job_definition is 'Operational job catalog synchronized to Quartz';
comment on table job_execution is 'Unified scheduled and manual job execution history';

insert into job_definition
    (id, version, code, display_name, description, handler_key, cron_expression, time_zone, enabled)
values
    ('b981498a-122b-48a1-851d-338259c0f2a1', 0, 'notification-dispatch',
     'Notification outbox dispatcher',
     'Claims due notification outbox records and delegates delivery to the notification module',
     'notification-dispatch', '0/5 * * * * ?', 'Asia/Tehran', true),
    ('b981498a-122b-48a1-851d-338259c0f2a2', 0, 'inquiry-dispatch',
     'Inquiry dispatcher',
     'Executes due provider requests and delivers completion callbacks for every inquiry consumer',
     'inquiry-dispatch', '0/15 * * * * ?', 'Asia/Tehran', true);
