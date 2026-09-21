package ir.jetvam.common.exception;

import java.util.Map;

/** Base type for expected rule or use-case failures. */
public class BusinessException extends JetvamException {

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, ?> details) {
        super(errorCode, message, null, details);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause, Map<String, ?> details) {
        super(errorCode, message, cause, details);
    }
}
