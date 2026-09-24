package ir.jetvam.modules.integration.routing;

/**
 * Adapts one provider-specific protocol to a canonical Jetvam capability command.
 * Wire DTOs, provider signatures and response mapping remain hidden behind implementations.
 *
 * @param <C> canonical command type
 * @param <R> canonical result type
 * @author reza jamshidi
 * @since 9/24/2026
 */
public interface ExternalProviderAdapter<C, R> {

    String capabilityCode();

    String adapterCode();

    Class<C> commandType();

    R execute(C command, ProviderInvocationContext context);
}
