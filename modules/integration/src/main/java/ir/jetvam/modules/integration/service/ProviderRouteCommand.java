package ir.jetvam.modules.integration.service;

import ir.jetvam.modules.integration.model.ProviderRoutingMode;

/**
 * Carries capability routing and circuit-breaker settings from administration.
 * Manual overrides are intentionally managed through a separate privileged operation.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record ProviderRouteCommand(
        ProviderRoutingMode routingMode,
        boolean failoverEnabled,
        int failureThreshold,
        long openDurationSeconds
) {
}
