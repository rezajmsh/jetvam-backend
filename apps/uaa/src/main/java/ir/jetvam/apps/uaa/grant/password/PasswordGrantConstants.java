package ir.jetvam.apps.uaa.grant.password;

import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * OAuth extension used by password accounts with policy-driven OTP.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public final class PasswordGrantConstants {

    public static final String GRANT_TYPE_VALUE = "urn:jetvam:params:oauth:grant-type:password";
    public static final AuthorizationGrantType GRANT_TYPE = new AuthorizationGrantType(GRANT_TYPE_VALUE);
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String CHALLENGE_ID = "challenge_id";
    public static final String OTP = "otp";
    public static final String SCOPE = "scope";

    private PasswordGrantConstants() {
    }
}
