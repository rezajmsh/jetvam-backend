package ir.jetvam.apps.uaa.customer;

import jakarta.validation.constraints.NotBlank;

/**
 * Accepts a customer mobile number for passwordless login initiation.
 * The response is uniform whether or not an eligible account exists.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record RequestCustomerLoginOtp(@NotBlank String mobile) {
}
