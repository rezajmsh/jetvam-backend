package ir.jetvam.common.exception;

import java.util.Map;

/** Base type for failures that are not caused by a rejected business decision. */
public class TechnicalException extends JetvamException {

    public TechnicalException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public TechnicalException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public TechnicalException(ErrorCode errorCode, String message, Throwable cause, Map<String, ?> details) {
        super(errorCode, message, cause, details);
    }
}
