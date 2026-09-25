# Jetvam repository guidance

Before changing business behavior, read `docs/architecture/jetvam-business-context.md`.

- Treat the manual and customer-journey video as descriptions of the current production journey.
- Treat the SRS as target-state roadmap requirements, not proof that a capability already exists.
- Preserve the distinction between current behavior and future requirements in code, migrations, and documentation.
- Authentication and authorization changes must follow `docs/security/frontend-authentication.md`.
- Every protected service operation must check both an appropriate role and a granular permission. Ownership checks such as `self` or merchant scope remain business-layer responsibilities and must not be replaced by role checks alone.
- All calls to external systems must follow `docs/architecture/external-integration-guidelines.md`: consumer modules depend on the consumer-independent `jetvam-module-integration` and own their provider protocol mappings; shared routing, authentication, TLS/mTLS, failover and runtime configuration remain in integration, which must never depend on consumers.
- Never perform external network I/O while a database transaction is open. Split the workflow into short database phases around the external call and make the completion phase idempotent or concurrency-safe.
- Every top-level Java type must have class-level JavaDoc. Put its summary and description before the block tags, then include `@author reza jamshidi` and an `@since M/d/yyyy` value matching the type's creation date. JavaDoc has no standard `@description` tag; the leading paragraph is the rendered description.
