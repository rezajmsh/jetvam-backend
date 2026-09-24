package ir.jetvam.modules.integration.service;

import ir.jetvam.modules.integration.model.ProviderRoutingMode;

import java.time.Instant;

/**
 * Exposes an immutable routing policy to administration and the runtime router.
 * The version participates in cache invalidation across configuration changes.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record ProviderRouteView(
        String capabilityCode,
        ProviderRoutingMode routingMode,
        boolean failoverEnabled,
        String forcedProviderCode,
        Instant forcedUntil,
        int failureThreshold,
        long openDurationSeconds,
        long version
) {
    public boolean hasActiveOverride(Instant now) {
        return forcedProviderCode != null && forcedUntil != null && forcedUntil.isAfter(now);
    }
}
