package ir.jetvam.modules.integration.service;

import ir.jetvam.modules.integration.model.ProviderAuthenticationType;

/**
 * Supplies validated provider configuration without exposing mutable persistence state.
 * Secret references are resolved only at the outbound HTTP boundary.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record ExternalProviderView(
        String capabilityCode,
        String providerCode,
        String adapterCode,
        boolean enabled,
        int priority,
        int weight,
        String baseUrl,
        String operationPath,
        ProviderAuthenticationType authenticationType,
        String authenticationHeader,
        String authenticationUsername,
        String credentialSecretRef,
        String tlsProfileCode,
        long connectTimeoutMillis,
        long readTimeoutMillis,
        String metadataJson,
        long version
) {
}
