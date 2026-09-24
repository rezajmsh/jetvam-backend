package ir.jetvam.modules.integration.service;

import ir.jetvam.modules.integration.model.ProviderAuthenticationType;

/**
 * Defines the complete mutable configuration for one provider binding.
 * APIs use replacement semantics so partial updates cannot leave invalid security state.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record ProviderDefinitionCommand(
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
        String metadataJson
) {
}
