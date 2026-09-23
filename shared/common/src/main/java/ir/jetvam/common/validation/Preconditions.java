package ir.jetvam.common.validation;

import ir.jetvam.common.text.TextUtils;

/**
 * Fail-fast guards for programming, configuration and infrastructure invariants.
 * Domain-input validation should use {@link ValidationCollector} instead.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class Preconditions {

    private Preconditions() {
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(requireMessage(message));
        }
    }

    public static <T> T requireNonNull(T value, String argumentName) {
        if (value == null) {
            throw new IllegalArgumentException(label(argumentName) + " must not be null");
        }
        return value;
    }

    public static String requireText(CharSequence value, String argumentName) {
        if (!TextUtils.hasText(value)) {
            throw new IllegalArgumentException(label(argumentName) + " must not be blank");
        }
        return value.toString();
    }

    public static int requirePositive(int value, String argumentName) {
        require(value > 0, label(argumentName) + " must be greater than zero");
        return value;
    }

    public static long requirePositive(long value, String argumentName) {
        require(value > 0, label(argumentName) + " must be greater than zero");
        return value;
    }

    public static int requireNonNegative(int value, String argumentName) {
        require(value >= 0, label(argumentName) + " must not be negative");
        return value;
    }

    public static long requireNonNegative(long value, String argumentName) {
        require(value >= 0, label(argumentName) + " must not be negative");
        return value;
    }

    public static int requireInRange(int value, int minimum, int maximum, String argumentName) {
        require(minimum <= maximum, "minimum must not be greater than maximum");
        require(
                value >= minimum && value <= maximum,
                label(argumentName) + " must be between " + minimum + " and " + maximum
        );
        return value;
    }

    private static String label(String argumentName) {
        if (!TextUtils.hasText(argumentName)) {
            throw new IllegalArgumentException("argumentName must not be blank");
        }
        return argumentName;
    }

    private static String requireMessage(String message) {
        if (!TextUtils.hasText(message)) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return message;
    }
}
