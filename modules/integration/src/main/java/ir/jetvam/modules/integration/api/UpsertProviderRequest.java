package ir.jetvam.modules.integration.api;

import ir.jetvam.modules.integration.model.ProviderAuthenticationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Validates a complete provider definition submitted by an administrator.
 * Credential input is restricted to an external secret reference.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record UpsertProviderRequest(
        @NotBlank String adapterCode,
        boolean enabled,
        @Min(0) int priority,
        @Positive int weight,
        @NotBlank String baseUrl,
        @NotBlank String operationPath,
        @NotNull ProviderAuthenticationType authenticationType,
        String authenticationHeader,
        String authenticationUsername,
        String credentialSecretRef,
        String tlsProfileCode,
        @Positive long connectTimeoutMillis,
        @Positive long readTimeoutMillis,
        String metadataJson
) {
}
