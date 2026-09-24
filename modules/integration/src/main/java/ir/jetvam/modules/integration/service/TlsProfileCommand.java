package ir.jetvam.modules.integration.service;

/**
 * Defines trust and client-key material for a named runtime TLS profile.
 * Password fields contain secret references rather than their resolved values.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record TlsProfileCommand(
        String storeType,
        String trustStoreLocation,
        String trustStorePasswordRef,
        String keyStoreLocation,
        String keyStorePasswordRef,
        String keyPasswordRef,
        String enabledProtocols
) {
}
