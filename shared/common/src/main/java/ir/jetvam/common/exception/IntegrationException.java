package ir.jetvam.common.exception;

import java.util.Map;

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
