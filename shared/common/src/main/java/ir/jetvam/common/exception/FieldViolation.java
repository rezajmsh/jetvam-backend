package ir.jetvam.common.exception;

import java.util.Objects;

/** Rejected values are intentionally excluded to avoid leaking sensitive data. */
public record FieldViolation(String field, String code, String message) {

    public FieldViolation {
        field = Objects.requireNonNull(field, "field must not be null");
        code = Objects.requireNonNull(code, "code must not be null");
        message = Objects.requireNonNull(message, "message must not be null");
    }
}
