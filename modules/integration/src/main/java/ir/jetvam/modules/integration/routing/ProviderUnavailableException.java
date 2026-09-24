package ir.jetvam.modules.integration.routing;

/**
 * Signals that routing found no configured and healthy provider for a capability.
 * The exception contains no endpoint, credential or customer data.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(String capabilityCode) {
        super("No external provider is available for capability " + capabilityCode);
    }
}
