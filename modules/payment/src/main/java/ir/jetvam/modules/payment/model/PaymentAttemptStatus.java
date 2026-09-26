package ir.jetvam.modules.payment.model;

/**
 * Tracks one payment attempt independently from the underlying fee obligation.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public enum PaymentAttemptStatus {
    CREATED,
    SUCCEEDED,
    FAILED
}
