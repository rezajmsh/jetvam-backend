package ir.jetvam.modules.identity.exception;

import ir.jetvam.common.exception.BusinessException;
import ir.jetvam.modules.identity.IdentityErrorCode;

/**
 * Indicates that a registered customer account cannot currently authenticate.
 * Disabled, pending and locked accounts share one public response without leaking state.
 *
 * @author reza jamshidi
 * @since 9/27/2026
 */
public final class CustomerAccountUnavailableException extends BusinessException {

    public CustomerAccountUnavailableException() {
        super(
                IdentityErrorCode.CUSTOMER_ACCOUNT_UNAVAILABLE,
                "Customer account is not available for sign-in; contact support"
        );
    }
}
