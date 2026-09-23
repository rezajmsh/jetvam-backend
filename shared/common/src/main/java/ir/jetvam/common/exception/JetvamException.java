package ir.jetvam.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Root of all expected application exceptions.
 *
 * <p>The exception deliberately has no HTTP concern. Web adapters are responsible
 * for mapping an error code and exception subtype to a transport status.</p>
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public abstract class JetvamException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    protected JetvamException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, Map.of());
    }

    protected JetvamException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, Map.of());
    }

    protected JetvamException(
            ErrorCode errorCode,
            String message,
            Throwable cause,
            Map<String, ?> details
    ) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.code = Objects.requireNonNull(errorCode, "errorCode must not be null").code();
        this.details = immutableDetails(details);
    }

    public final String code() {
        return code;
    }

    public final Map<String, Object> details() {
        return details;
    }

    private static Map<String, Object> immutableDetails(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        var copy = new LinkedHashMap<String, Object>();
        source.forEach((key, value) -> copy.put(Objects.requireNonNull(key, "detail key must not be null"), value));
        return Map.copyOf(copy);
    }
}
