package ir.jetvam.modules.identity.service;

/**
 * Returns the trusted identifiers bound to a successfully consumed OTP challenge.
 * Callers never accept mobile or national code again during verification.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record OtpVerificationData(String mobile, String nationalCode) {
}
