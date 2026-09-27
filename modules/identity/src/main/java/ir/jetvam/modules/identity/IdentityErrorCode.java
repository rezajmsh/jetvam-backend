package ir.jetvam.modules.identity;

import ir.jetvam.common.exception.ErrorCode;

/**
 * Defines stable machine-readable failures owned by the identity module.
 * Client applications use these codes to choose the correct authentication flow.
 *
 * @author reza jamshidi
 * @since 9/27/2026
 */
public enum IdentityErrorCode implements ErrorCode {
    CUSTOMER_REGISTRATION_REQUIRED("IDENTITY.CUSTOMER_REGISTRATION_REQUIRED"),
    CUSTOMER_ACCOUNT_UNAVAILABLE("IDENTITY.CUSTOMER_ACCOUNT_UNAVAILABLE");

    private final String code;

    IdentityErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
