package ir.jetvam.common.exception;

import java.util.Map;

public final class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(CommonErrorCode.RESOURCE_CONFLICT, message);
    }

    public ConflictException(String message, Map<String, ?> details) {
        super(CommonErrorCode.RESOURCE_CONFLICT, message, details);
    }
}
