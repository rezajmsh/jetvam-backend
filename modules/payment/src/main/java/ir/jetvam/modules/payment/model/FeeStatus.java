package ir.jetvam.modules.payment.model;

/**
 * Tracks the settlement state of a single immutable fee obligation.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public enum FeeStatus {
    BLOCKED,
    PENDING,
    PAID,
    CANCELLED,
    REFUNDED
}
