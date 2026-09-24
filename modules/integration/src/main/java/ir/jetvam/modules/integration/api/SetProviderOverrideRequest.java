package ir.jetvam.modules.integration.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Defines a time-bounded manual provider override for operational recovery.
 * Expiry prevents emergency routing decisions from silently becoming permanent.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record SetProviderOverrideRequest(
        @NotBlank String providerCode,
        @NotNull @Future Instant until
) {
}
