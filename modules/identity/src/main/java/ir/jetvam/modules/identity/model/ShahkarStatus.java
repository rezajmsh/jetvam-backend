package ir.jetvam.modules.identity.model;

/**
 * Records the result of matching a mobile number with a national identifier.
 * The status is retained separately from general identity verification.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public enum ShahkarStatus {
    NOT_REQUESTED,
    PENDING,
    MATCHED,
    NOT_MATCHED,
    FAILED
}
