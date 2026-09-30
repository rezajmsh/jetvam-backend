package ir.jetvam.modules.identity.model;

/**
 * Tracks authoritative identity verification without coupling it to customer onboarding stages.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public enum IdentityVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
