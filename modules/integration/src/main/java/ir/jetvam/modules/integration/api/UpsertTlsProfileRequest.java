package ir.jetvam.modules.integration.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Validates a runtime TLS profile while keeping all passwords as secret references.
 * Trust-only TLS and mutual TLS are both represented by optional store locations.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record UpsertTlsProfileRequest(
        @NotBlank String storeType,
        String trustStoreLocation,
        String trustStorePasswordRef,
        String keyStoreLocation,
        String keyStorePasswordRef,
        String keyPasswordRef,
        @NotBlank String enabledProtocols
) {
}
