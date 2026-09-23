package ir.jetvam.apps.uaa.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Accepts the challenge reference and one-time code for customer registration.
 * Mobile and national code are intentionally not accepted a second time.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record VerifyCustomerRegistrationRequest(@NotNull UUID challengeId, @NotBlank String otp) {
}
