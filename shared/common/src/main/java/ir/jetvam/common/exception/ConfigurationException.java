package ir.jetvam.common.exception;

import java.util.Map;

/**
 * Represents a configuration failure in the common exception hierarchy.
 * Its stable error code can be mapped consistently at API boundaries.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class ConfigurationException extends TechnicalException {

    public ConfigurationException(String message) {
        super(CommonErrorCode.CONFIGURATION_FAILURE, message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(CommonErrorCode.CONFIGURATION_FAILURE, message, cause);
    }

    public ConfigurationException(String property, String message) {
        this(property, message, null);
    }

    public ConfigurationException(String property, String message, Throwable cause) {
        super(
                CommonErrorCode.CONFIGURATION_FAILURE,
                message,
                cause,
                Map.of("property", property)
        );
    }
}
