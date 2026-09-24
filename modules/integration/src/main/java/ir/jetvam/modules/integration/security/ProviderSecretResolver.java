package ir.jetvam.modules.integration.security;

/**
 * Resolves provider credentials from deployment-controlled secret references.
 * Implementations may add Vault or cloud secret managers without changing adapters.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public interface ProviderSecretResolver {

    String resolve(String reference);

    String fingerprint(String reference);
}
