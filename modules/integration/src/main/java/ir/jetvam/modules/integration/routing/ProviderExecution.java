package ir.jetvam.modules.integration.routing;

/**
 * Returns a provider result together with the selected provider identity.
 * Stateful multi-step consumers persist the provider code and route subsequent calls back to it.
 *
 * @param providerCode selected provider code
 * @param result normalized adapter result
 * @param <R> result type
 * @author reza jamshidi
 * @since 9/25/2026
 */
public record ProviderExecution<R>(String providerCode, R result) {
}
