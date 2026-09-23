package ir.jetvam.common.exception;

/**
 * Defines stable error codes emitted by the common exception hierarchy.
 * Codes remain independent from localized human-readable messages.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public enum CommonErrorCode implements ErrorCode {
    VALIDATION_FAILED("COMMON.VALIDATION_FAILED"),
    TYPE_CONVERSION_FAILED("COMMON.TYPE_CONVERSION_FAILED"),
    RESOURCE_NOT_FOUND("COMMON.RESOURCE_NOT_FOUND"),
    RESOURCE_CONFLICT("COMMON.RESOURCE_CONFLICT"),
    RATE_LIMIT_EXCEEDED("COMMON.RATE_LIMIT_EXCEEDED"),
    OPERATION_NOT_ALLOWED("COMMON.OPERATION_NOT_ALLOWED"),
    ILLEGAL_STATE_TRANSITION("COMMON.ILLEGAL_STATE_TRANSITION"),
    TECHNICAL_FAILURE("COMMON.TECHNICAL_FAILURE"),
    INTEGRATION_FAILURE("COMMON.INTEGRATION_FAILURE"),
    CONFIGURATION_FAILURE("COMMON.CONFIGURATION_FAILURE");

    private final String code;

    CommonErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
