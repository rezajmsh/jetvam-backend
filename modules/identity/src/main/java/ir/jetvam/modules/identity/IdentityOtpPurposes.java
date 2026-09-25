package ir.jetvam.modules.identity;

import ir.jetvam.modules.otp.OtpPurpose;

/**
 * Defines identity-owned OTP purposes and their notification templates.
 * Keeping these constants outside the OTP module preserves its business neutrality.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public final class IdentityOtpPurposes {

    public static final OtpPurpose CUSTOMER_REGISTRATION = new OtpPurpose(
            "CUSTOMER_REGISTRATION",
            "identity.otp.customer-registration"
    );
    public static final OtpPurpose CUSTOMER_LOGIN = new OtpPurpose(
            "CUSTOMER_LOGIN",
            "identity.otp.customer-login"
    );
    public static final OtpPurpose PASSWORD_LOGIN_SECOND_FACTOR = new OtpPurpose(
            "PASSWORD_LOGIN_SECOND_FACTOR",
            "identity.otp.password-login-second-factor"
    );

    private IdentityOtpPurposes() {
    }
}
