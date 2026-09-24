package ir.jetvam.modules.integration.routing;

import java.time.Instant;

/**
 * Exposes bounded operational circuit state without leaking provider payloads.
 * Administrators can inspect and reset a provider independently of its persisted configuration.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record ProviderCircuitView(
        String capabilityCode,
        String providerCode,
        int consecutiveFailures,
        Instant openUntil,
        boolean halfOpenTrialRunning
) {
    public boolean open(Instant now) {
        return openUntil != null && openUntil.isAfter(now);
    }
}
