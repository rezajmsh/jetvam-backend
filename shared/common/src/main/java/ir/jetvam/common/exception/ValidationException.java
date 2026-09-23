package ir.jetvam.common.exception;

import java.util.List;
import java.util.Map;

/**
 * Represents a validation failure in the common exception hierarchy.
 * Its stable error code can be mapped consistently at API boundaries.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public class ValidationException extends BusinessException {

    private final List<FieldViolation> violations;

    public ValidationException(String message) {
        this(CommonErrorCode.VALIDATION_FAILED, message, List.of());
    }

    public ValidationException(String message, List<FieldViolation> violations) {
        this(CommonErrorCode.VALIDATION_FAILED, message, violations);
    }

    public ValidationException(ErrorCode errorCode, String message, List<FieldViolation> violations) {
        this(errorCode, message, violations, null);
    }

    protected ValidationException(
            ErrorCode errorCode,
            String message,
            List<FieldViolation> violations,
            Throwable cause
    ) {
        super(errorCode, message, cause, Map.of("violationCount", violations == null ? 0 : violations.size()));
        this.violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public List<FieldViolation> violations() {
        return violations;
    }
}
