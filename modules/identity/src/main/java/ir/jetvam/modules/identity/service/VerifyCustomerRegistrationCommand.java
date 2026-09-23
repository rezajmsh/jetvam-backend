package ir.jetvam.modules.identity.service;

import java.util.UUID;

/**
 * Completes the OTP step using only the challenge identifier and submitted code.
 * Identity values are recovered from the trusted persisted challenge.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record VerifyCustomerRegistrationCommand(UUID challengeId, String otp) {
}
