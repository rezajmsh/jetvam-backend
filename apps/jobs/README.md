# Jetvam Jobs Application

`jetvam-jobs-app` owns operational job definitions, runtime scheduling, manual triggering, and execution history.
It does not contain business workflows. Each workload registers a `JobHandler`; the handler delegates to the
service exposed by the business module that owns the use case.

Definitions are stored in `job_definition`. Every scheduled and manual attempt is stored in `job_execution`.
The application registers enabled database definitions with Spring's task scheduler on startup. A database lease
on each definition prevents overlapping execution across multiple jobs application instances. Disabling a definition
removes only its runtime cron registration; authorized manual execution remains available.

Each execution stores item-level `processed_count`, `succeeded_count`, and `failed_count` values in addition to
its overall scheduler status. Handlers return those counters through `JobResult`. Cumulative totals for a job are
available from `GET /api/v1/jobs/{id}/statistics`.

Cron expressions use Spring's six-field format, for example `0/5 * * * * ?` for every five seconds.

The baseline schedules two independent workloads: `notification-dispatch` claims the encrypted notification
outbox, and `inquiry-dispatch` executes provider requests, multi-step polling, and durable callbacks. Inquiry
item details remain in `inquiry_request`; job-level counters and failures remain in `job_execution`.
