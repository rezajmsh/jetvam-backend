# Jetvam observability infrastructure

This module provides the framework-neutral observability services used by web,
jobs, persistence, and business modules. Adding the dependency enables its Spring
Boot auto-configuration; every feature can be disabled independently under
`jetvam.observability`.

## Signals

| Signal | Event or metric | Default |
| --- | --- | --- |
| Inbound HTTP log | `http.server.request.started/completed` | enabled in `infra-web` |
| Outbound HTTP log | `http.client.request.completed` | enabled for injected `RestClient.Builder` and `RestTemplate` beans |
| Repository span/log | `repository.operation` / `repository.operation.completed` | enabled |
| Audit log | `audit.<action>` | enabled, explicit API call required |
| HTTP server timer | `http.server.request.duration` | enabled |
| HTTP client timer | `http.client.request.duration` | enabled |
| Repository timer | `repository.operation.duration` | enabled |
| Audit counter | `audit.events` | enabled |

JVM, process, HikariCP, Spring Cache, and standard Spring MVC observations are
provided by Actuator/Micrometer. Prometheus is exposed by the applications and
traces can be exported to an OTLP collector.

## Audit usage

```java
auditLogger.record(AuditEvent.builder()
        .action("loan-request.approved")
        .outcome("success")
        .actorType("bank-operator")
        .actorId(operatorId)
        .subjectType("loan-request")
        .subjectId(requestId)
        .attributes(Map.of("program.code", programCode))
        .build());
```

Audit events are deliberate business events. They must not be inferred from every
method invocation. Passwords, OTPs, tokens, authorization headers, request bodies,
and raw identity data must never be added to audit attributes.

## Log contract

`OtelStructuredLogFormatter` emits one JSON object per line with the OTel-aligned
fields `timestamp`, `observed_timestamp`, `severity_number`, `severity_text`,
`body`, `trace_id`, `span_id`, `resource`, and `attributes`. Infrastructure event
kind is available as `attributes.event.type`.

Request/response bodies and query strings are excluded by default. Header logging
uses an allow-list. `client-ip-header` is empty by default and should only be set
when the application is behind a trusted proxy.

OTLP export is disabled in the local application configuration. Enable it using
`JETVAM_OTEL_TRACES_EXPORT_ENABLED=true` and configure `OTEL_EXPORTER_OTLP_ENDPOINT`
or the signal-specific OpenTelemetry environment variables.
