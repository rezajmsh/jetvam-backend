package ir.jetvam.modules.integration.service;

/**
 * Carries a versioned TLS profile to the dynamic client factory.
 * Locations and secret references may change at runtime without application deployment.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record TlsProfileView(
        String profileCode,
        String storeType,
        String trustStoreLocation,
        String trustStorePasswordRef,
        String keyStoreLocation,
        String keyStorePasswordRef,
        String keyPasswordRef,
        String enabledProtocols,
        long version
) {
}
