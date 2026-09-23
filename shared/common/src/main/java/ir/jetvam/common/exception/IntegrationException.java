package ir.jetvam.common.exception;

import java.util.Map;

/**
 * Represents a integration failure in the common exception hierarchy.
 * Its stable error code can be mapped consistently at API boundaries.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class IntegrationException extends TechnicalException {

    public IntegrationException(String provider, String operation, Throwable cause) {
        super(
                CommonErrorCode.INTEGRATION_FAILURE,
                "External provider operation failed",
                cause,
                Map.of("provider", provider, "operation", operation)
        );
    }
}
