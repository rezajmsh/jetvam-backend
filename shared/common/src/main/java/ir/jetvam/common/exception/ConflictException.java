package ir.jetvam.common.exception;

import java.util.Map;

/**
 * Represents a conflict failure in the common exception hierarchy.
 * Its stable error code can be mapped consistently at API boundaries.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(CommonErrorCode.RESOURCE_CONFLICT, message);
    }

    public ConflictException(String message, Map<String, ?> details) {
        super(CommonErrorCode.RESOURCE_CONFLICT, message, details);
    }
}
