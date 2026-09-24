package ir.jetvam.modules.integration.api;

import ir.jetvam.modules.integration.model.ProviderRoutingMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Validates routing and circuit policy updates for one external capability.
 * Provider overrides are intentionally excluded and require a stronger permission.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record UpdateProviderRouteRequest(
        @NotNull ProviderRoutingMode routingMode,
        boolean failoverEnabled,
        @Positive int failureThreshold,
        @Positive long openDurationSeconds
) {
}
