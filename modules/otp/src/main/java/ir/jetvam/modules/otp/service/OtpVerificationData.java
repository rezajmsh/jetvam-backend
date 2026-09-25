package ir.jetvam.modules.otp.service;

/**
 * Returns trusted identifiers bound to a validated or consumed OTP challenge.
 * Callers never accept those identifiers again during the protected operation.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record OtpVerificationData(String mobile, String nationalCode) {
}
