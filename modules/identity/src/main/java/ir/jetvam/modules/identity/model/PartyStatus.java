package ir.jetvam.modules.identity.model;

/**
 * Represents the lifecycle status of a person or organization identity.
 * Suspended parties remain stored for audit and historical references.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public enum PartyStatus {
    ACTIVE,
    SUSPENDED,
    ARCHIVED
}
