# Jetvam HTTP Client Infrastructure

`jetvam-infra-http-client` provides named `RestClient` instances with centralized connect/read timeouts,
Micrometer trace propagation, OTel-compatible structured logs, and bounded metrics.

Create clients through `JetvamHttpClientFactory`; do not construct provider clients in application modules.
Request/response bodies, query values, credentials, and authorization headers are deliberately never logged.

```yaml
jetvam:
  http-client:
    connect-timeout: 3s
    read-timeout: 5s
    clients:
      shahkar:
        read-timeout: 8s
        observability-enabled: true
```

Each completed call emits `http.client.request.completed` and `http.client.request.duration` with the client
name, method, server, status, outcome, and duration. Standard Spring observations carry the active trace context.
