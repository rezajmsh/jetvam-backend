package ir.jetvam.modules.otp;

/**
 * Represents the security lifecycle of a persisted OTP challenge.
 * Terminal states prevent replay after success, expiry or exhausted attempts.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public enum OtpStatus {
    ACTIVE,
    CONSUMED,
    EXPIRED,
    ATTEMPTS_EXHAUSTED
}
