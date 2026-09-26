# Jetvam Integration Module

This module owns only the generic outbound-provider infrastructure independently of UAA, Jobs and business modules.
Consumer modules own their capability contract, canonical command/result, capability code and provider adapters.
Integration owns routing, authentication, HTTP clients, TLS/mTLS, circuit state and runtime configuration, and never
depends on a consumer.

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

## Configure an Inquiry-owned Shahkar adapter

The mobile-ownership contract, its canonical command/result and `GenericJsonMobileOwnershipAdapter` belong to Inquiry. Integration only
stores and executes the following runtime provider definition through its generic API.

The baseline seeds an enabled provider named `SHAHKAR_MOCK` with adapter code `SHAHKAR_MOCK_V1`.
It returns a successful ownership result without network I/O and is selected through the same database-backed router as real providers.

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
contract matches. When its request, response or signature differs, Inquiry supplies another adapter implementation;
no Shahkar type or conditional is added to Integration.

To switch UAA to the real provider, create or update `SHAHKAR_PRIMARY` with the real endpoint and credentials, enable it,
and disable `SHAHKAR_MOCK` through the provider management API. Registration continues to use `SHAHKAR_VERIFY`; no
configuration or business-code change is required.

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

1. Add a stable capability code to the owning consumer module.
2. Define the canonical command, result and small facade in that consumer.
3. Implement one consumer-owned adapter per distinct provider contract/signature.
4. Depend on Integration's generic router and HTTP client factory from those adapters.
5. Seed routes and development mocks only; add environment-specific real providers through the protected admin API.

For write operations, mark ambiguous timeouts as ambiguous and allow cross-provider failover only when the operation is
idempotent or can be reconciled. This prevents duplicate SMS messages, contracts and core-loan requests.
