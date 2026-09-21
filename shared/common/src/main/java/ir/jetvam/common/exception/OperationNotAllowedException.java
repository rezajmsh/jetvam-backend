package ir.jetvam.common.exception;

import java.util.Map;

public final class OperationNotAllowedException extends BusinessException {

    public OperationNotAllowedException(String operation, String reason) {
        super(
                CommonErrorCode.OPERATION_NOT_ALLOWED,
                reason,
                Map.of("operation", operation)
        );
    }
}
