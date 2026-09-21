package ir.jetvam.infra.web.api;

import ir.jetvam.common.exception.FieldViolation;

import java.util.List;
import java.util.Map;

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
