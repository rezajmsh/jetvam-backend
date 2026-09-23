package ir.jetvam.modules.identity.model;

/**
 * Tracks the customer registration journey independently from loan applications.
 * It allows OTP, identity completion and verification to progress explicitly.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public enum CustomerOnboardingStatus {
    MOBILE_PENDING,
    MOBILE_VERIFIED,
    IDENTITY_PENDING,
    IDENTITY_VERIFIED,
    COMPLETED,
    REJECTED
}
