package ir.jetvam.apps.uaa.security;

import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Defines the stable OAuth extension identifiers used by customer OTP login.
 * Portal clients reference these values at the standard token endpoint.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public final class OtpGrantConstants {

    public static final String GRANT_TYPE_VALUE = "urn:jetvam:params:oauth:grant-type:otp";
    public static final AuthorizationGrantType GRANT_TYPE = new AuthorizationGrantType(GRANT_TYPE_VALUE);
    public static final String CHALLENGE_ID = "challenge_id";
    public static final String OTP = "otp";
    public static final String SCOPE = "scope";

    private OtpGrantConstants() {
    }
}
