package ir.jetvam.common.validation;

import ir.jetvam.common.exception.FieldViolation;
import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Accumulates domain-input violations so callers can report all problems in one response.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class ValidationCollector {

    private final List<FieldViolation> violations = new ArrayList<>();

    private ValidationCollector() {
    }

    public static ValidationCollector create() {
        return new ValidationCollector();
    }

    public ValidationCollector require(boolean valid, String field, String code, String message) {
        if (!valid) {
            violations.add(new FieldViolation(field, code, message));
        }
        return this;
    }

    public ValidationCollector requireNotNull(Object value, String field) {
        return require(value != null, field, "REQUIRED", field + " is required");
    }

    public ValidationCollector requireText(CharSequence value, String field) {
        return require(TextUtils.hasText(value), field, "REQUIRED", field + " is required");
    }

    public ValidationCollector rejectIf(boolean rejected, String field, String code, String message) {
        return require(!rejected, field, code, message);
    }

    public ValidationCollector add(FieldViolation violation) {
        violations.add(Objects.requireNonNull(violation, "violation must not be null"));
        return this;
    }

    public boolean isValid() {
        return violations.isEmpty();
    }

    public List<FieldViolation> violations() {
        return List.copyOf(violations);
    }

    public void throwIfInvalid() {
        throwIfInvalid("Validation failed");
    }

    public void throwIfInvalid(String message) {
        if (!isValid()) {
            throw new ValidationException(message, violations);
        }
    }
}
