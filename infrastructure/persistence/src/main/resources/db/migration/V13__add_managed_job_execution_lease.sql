alter table job_definition
    add column if not exists lock_owner varchar(200),
    add column if not exists lock_until timestamp with time zone;

create index if not exists ix_job_definition_lock
    on job_definition (lock_until) where lock_until is not null;

comment on table job_definition is
    'Application-owned background job catalog and distributed execution lease';
