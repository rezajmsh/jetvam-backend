package ir.jetvam.modules.notification.delivery;

import lombok.Getter;

/**
 * Classifies a provider failure as retryable or permanently rejected.
 * Stable error codes are persisted without storing sensitive provider messages.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Getter
public class NotificationDeliveryException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;

    public NotificationDeliveryException(String errorCode, boolean retryable, Throwable cause) {
        super("Notification delivery failed", cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public static NotificationDeliveryException permanent(String errorCode, Throwable cause) {
        return new NotificationDeliveryException(errorCode, false, cause);
    }

    public static NotificationDeliveryException retryable(String errorCode, Throwable cause) {
        return new NotificationDeliveryException(errorCode, true, cause);
    }
}
