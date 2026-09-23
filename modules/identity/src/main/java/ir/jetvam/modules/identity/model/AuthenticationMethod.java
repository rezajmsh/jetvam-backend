package ir.jetvam.modules.identity.model;

/**
 * Identifies the primary credential accepted for a user account.
 * Additional factors can be introduced without changing the account identity.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public enum AuthenticationMethod {
    PASSWORD,
    OTP
}
