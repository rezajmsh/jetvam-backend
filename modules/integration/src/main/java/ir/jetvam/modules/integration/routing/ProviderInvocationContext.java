package ir.jetvam.modules.integration.routing;

import ir.jetvam.modules.integration.service.ExternalProviderView;
import ir.jetvam.modules.integration.service.TlsProfileView;

import java.util.Optional;

/**
 * Supplies versioned provider and optional TLS configuration to an adapter invocation.
 * It deliberately excludes business state and sensitive resolved credential values.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record ProviderInvocationContext(
        ExternalProviderView provider,
        Optional<TlsProfileView> tlsProfile
) {
}
