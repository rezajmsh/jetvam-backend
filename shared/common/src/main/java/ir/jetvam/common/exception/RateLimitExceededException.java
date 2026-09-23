package ir.jetvam.common.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a temporarily rejected operation after an application rate limit.
 * The retry timestamp allows HTTP and non-HTTP callers to schedule a safe retry.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public final class RateLimitExceededException extends BusinessException {

    public RateLimitExceededException(String message, Instant retryAt) {
        super(CommonErrorCode.RATE_LIMIT_EXCEEDED, message, Map.of("retryAt", retryAt.toString()));
    }
}
