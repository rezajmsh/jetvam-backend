package ir.jetvam.infra.web.api;

import ir.jetvam.common.exception.FieldViolation;

import java.util.List;
import java.util.Map;

/**
 * Carries a machine-readable API error with validation and diagnostic details.
 * The structure is shared by controller and security failures.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public record ApiError(
        String code,
        String message,
        List<FieldViolation> violations,
        Map<String, Object> details
) {
    public ApiError {
        violations = violations == null ? List.of() : List.copyOf(violations);
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
