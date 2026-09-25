package ir.jetvam.apps.uaa.grant.password;

import ir.jetvam.modules.otp.service.OtpChallengeView;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Signals that valid password credentials require a delivered OTP before token issuance.
 * The token failure handler exposes only safe challenge metadata to the frontend.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public final class PasswordSecondFactorRequiredException extends OAuth2AuthenticationException {

    public static final String ERROR_CODE = "second_factor_required";

    private final OtpChallengeView challenge;

    public PasswordSecondFactorRequiredException(OtpChallengeView challenge) {
        super(new OAuth2Error(
                ERROR_CODE,
                "A one-time password is required to complete authentication",
                null
        ));
        this.challenge = challenge;
    }

    public OtpChallengeView challenge() {
        return challenge;
    }
}
