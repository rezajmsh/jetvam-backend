# Jetvam Integration Module

This module owns outbound provider integration independently of UAA, Jobs and business consumer modules.
Consumers depend on canonical capabilities such as `ShahkarProvider`; provider DTOs, authentication, TLS and
routing remain internal to this module.

## Runtime model

- `integration_provider_route` selects `PRIORITY_FAILOVER`, `ROUND_ROBIN`, `WEIGHTED` or `MANUAL_ONLY` per capability.
- `integration_provider` binds a provider code to a compiled adapter and stores endpoint, timeout, auth and TLS references.
- `integration_tls_profile` defines JKS/PKCS12 trust stores and optional mutual-TLS key stores.
- Circuit state is isolated by `capability + provider` and can be inspected or reset through the admin API.
- Configuration rows are read for every routing decision. HTTP clients are rebuilt when a row version, secret file or
  certificate file fingerprint changes.

Passwords and tokens must not be persisted. `credentialSecretRef` and TLS password references support:

- `env:VARIABLE_NAME`
- `file:C:/secure/provider.secret` on Windows or `file:/run/secrets/provider` on Linux

A Vault or cloud secret manager can be added by replacing `ProviderSecretResolver` without changing provider adapters.

## Configure Shahkar

Create a provider through `PUT /api/v1/integrations/providers/SHAHKAR_VERIFY/SHAHKAR_PRIMARY`:

```json
{
  "adapterCode": "SHAHKAR_HTTP_JSON_V1",
  "enabled": true,
  "priority": 10,
  "weight": 100,
  "baseUrl": "https://shahkar.example.ir",
  "operationPath": "/v1/identity/shahkar",
  "authenticationType": "API_KEY",
  "authenticationHeader": "X-Api-Key",
  "authenticationUsername": null,
  "credentialSecretRef": "file:/run/secrets/shahkar-api-key",
  "tlsProfileCode": "SHAHKAR_MTLS",
  "connectTimeoutMillis": 3000,
  "readTimeoutMillis": 5000,
  "metadataJson": "{}"
}
```

Add a secondary provider using another provider code and a larger priority. It may use the same adapter when its wire
contract matches, or a new `ExternalProviderAdapter<ShahkarCommand, ShahkarVerification>` when its request, response or
cryptographic signature differs.

## Configure TLS or mutual TLS

Create or replace a profile through `PUT /api/v1/integrations/tls-profiles/SHAHKAR_MTLS`:

```json
{
  "storeType": "PKCS12",
  "trustStoreLocation": "file:/etc/jetvam/tls/shahkar-trust.p12",
  "trustStorePasswordRef": "file:/run/secrets/shahkar-trust-password",
  "keyStoreLocation": "file:/etc/jetvam/tls/shahkar-client.p12",
  "keyStorePasswordRef": "file:/run/secrets/shahkar-client-password",
  "keyPasswordRef": "file:/run/secrets/shahkar-key-password",
  "enabledProtocols": "TLSv1.3,TLSv1.2"
}
```

Replacing either file changes its fingerprint and rebuilds the client on the next invocation. The privileged
`POST /api/v1/integrations/clients/reload` endpoint provides an explicit reload when an external secret implementation
cannot expose a fingerprint.

## Manual operations

- Set a time-bounded override: `PUT /api/v1/integrations/routes/{capability}/override`
- Clear the override: `DELETE /api/v1/integrations/routes/{capability}/override`
- Inspect circuits: `GET /api/v1/integrations/circuits`
- Reset one circuit: `POST /api/v1/integrations/circuits/{capability}/{provider}/reset`

Read operations require `SYSTEM_ADMIN` or `SYSTEM_OPERATOR` plus `integration:provider:read`. Mutations require
`SYSTEM_ADMIN` plus the matching `integration:provider:write` or `integration:provider:override` permission.

## Adding a new external capability

1. Add a stable capability code.
2. Define a canonical command and result in this module.
3. Expose a small consumer-facing facade.
4. Implement one adapter per distinct provider contract/signature.
5. Seed only the route; add environment-specific providers through the protected admin API.

For write operations, mark ambiguous timeouts as ambiguous and allow cross-provider failover only when the operation is
idempotent or can be reconciled. This prevents duplicate SMS messages, contracts and core-loan requests.
