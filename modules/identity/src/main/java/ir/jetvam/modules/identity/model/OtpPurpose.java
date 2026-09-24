package ir.jetvam.modules.identity.model;

/**
 * Separates one-time passwords issued for registration from login credentials.
 * A challenge can only be consumed for the purpose for which it was created.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public enum OtpPurpose {
    CUSTOMER_REGISTRATION,
    CUSTOMER_LOGIN,
    PASSWORD_LOGIN_SECOND_FACTOR
}
