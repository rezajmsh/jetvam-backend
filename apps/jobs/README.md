# Jetvam Jobs Application

`jetvam-jobs-app` owns operational job definitions, Quartz scheduling, manual triggering, and execution history.
It does not contain business workflows. Each workload registers a `JobHandler`; the handler delegates to the
service exposed by the business module that owns the use case.

Definitions are stored in `job_definition`. Every scheduled and manual attempt is stored in `job_execution`.
Quartz uses its JDBC job store so scheduling and non-concurrent execution continue to work in a clustered jobs
application. A disabled definition keeps its durable Quartz job, allowing authorized manual execution while its
cron trigger is removed.

Each execution stores item-level `processed_count`, `succeeded_count`, and `failed_count` values in addition to
its overall scheduler status. Handlers return those counters through `JobResult`. Cumulative totals for a job are
available from `GET /api/v1/jobs/{id}/statistics`.

Quartz cron expressions use six or seven fields, for example `0/5 * * * * ?` for every five seconds.
