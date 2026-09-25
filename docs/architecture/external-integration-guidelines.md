# External integration rules

These rules apply to every current and future integration with an external system, including SMS, Shahkar, credit cores, payment providers and inquiry services.

## Ownership

- Business modules own or consume provider-neutral capability contracts; they must not create `RestClient`, TLS contexts or provider-specific request signatures.
- `jetvam-module-integration` is the consumer-independent core for routing, failover, circuit state, authentication, TLS/mTLS and runtime provider configuration. It must never depend on a `jetvam-module-*` consumer.
- A consumer module that invokes external providers depends on `jetvam-module-integration` and owns the adapters that map its canonical model to provider wire formats.
- A provider-specific signature belongs in a consumer-owned adapter selected by `adapterCode`. Adding a provider must not add provider conditionals to business services or to the integration core.
- Secrets are referenced through resolvers and are never stored as plaintext in provider definitions.
- Provider, route and TLS changes must be reloadable at runtime without an application release.

## Transaction boundary

External network I/O must never execute inside a database transaction. Use three explicit phases:

1. Validate and persist the local intent in a short transaction when required.
2. Call the external capability after that transaction has completed.
3. Persist the result in a new short, concurrency-safe transaction.

For synchronous flows, validate replay-sensitive credentials before the call and consume them atomically in the completion transaction. For asynchronous flows, prefer an outbox or job command with an idempotency key.

## Reliability

- Configure connection/read timeouts for every provider.
- Consumer-owned adapters route through the shared provider router so priority, weighted, round-robin, manual override and failover policies remain consistent.
- Treat only retryable transport/provider failures as failover candidates; business rejections must be returned without trying another provider unless the capability explicitly defines otherwise.
- Emit provider/capability metrics and logs without credentials or sensitive payloads.
- Tests must cover adapter mapping, route selection, failover behavior and transaction-boundary orchestration.
