package ir.jetvam.modules.identity.model;

/**
 * Controls whether an account can authenticate and receive new tokens.
 * Locked and disabled states are kept distinct for operational handling.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public enum UserAccountStatus {
    PENDING,
    ACTIVE,
    LOCKED,
    DISABLED
}
