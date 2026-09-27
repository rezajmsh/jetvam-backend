package ir.jetvam.modules.identity.exception;

import ir.jetvam.common.exception.BusinessException;
import ir.jetvam.modules.identity.IdentityErrorCode;

/**
 * Indicates that passwordless login cannot start before customer registration completes.
 * No mobile number is included in the exception details or observability output.
 *
 * @author reza jamshidi
 * @since 9/27/2026
 */
public final class CustomerRegistrationRequiredException extends BusinessException {

    public CustomerRegistrationRequiredException() {
        super(
                IdentityErrorCode.CUSTOMER_REGISTRATION_REQUIRED,
                "Customer registration must be completed before signing in"
        );
    }
}
