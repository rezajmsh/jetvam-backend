package ir.jetvam.common.exception;

import java.util.Map;

/**
 * Represents a operation not allowed failure in the common exception hierarchy.
 * Its stable error code can be mapped consistently at API boundaries.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class OperationNotAllowedException extends BusinessException {

    public OperationNotAllowedException(String operation, String reason) {
        super(
                CommonErrorCode.OPERATION_NOT_ALLOWED,
                reason,
                Map.of("operation", operation)
        );
    }
}
