package ir.jetvam.modules.integration.routing;

import lombok.Getter;

/**
 * Normalizes provider failures for retry, circuit and failover decisions.
 * Ambiguous failures stop unsafe write operations unless they are explicitly idempotent.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Getter
public class ProviderInvocationException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;
    private final boolean ambiguous;

    public ProviderInvocationException(
            String errorCode,
            boolean retryable,
            boolean ambiguous,
            Throwable cause
    ) {
        super("External provider invocation failed: " + errorCode, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.ambiguous = ambiguous;
    }

    public static ProviderInvocationException retryable(String errorCode, boolean ambiguous, Throwable cause) {
        return new ProviderInvocationException(errorCode, true, ambiguous, cause);
    }

    public static ProviderInvocationException permanent(String errorCode, Throwable cause) {
        return new ProviderInvocationException(errorCode, false, false, cause);
    }
}
