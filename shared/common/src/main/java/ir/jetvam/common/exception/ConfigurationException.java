package ir.jetvam.common.exception;

import java.util.Map;

public final class ConfigurationException extends TechnicalException {

    public ConfigurationException(String property, String message) {
        super(
                CommonErrorCode.CONFIGURATION_FAILURE,
                message,
                null,
                Map.of("property", property)
        );
    }
}
