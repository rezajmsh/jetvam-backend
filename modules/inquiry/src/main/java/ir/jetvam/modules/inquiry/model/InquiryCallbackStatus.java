package ir.jetvam.modules.inquiry.model;

/**
 * Tracks durable at-least-once delivery of a terminal inquiry outcome.
 * A transport may be an in-process Spring handler today or an external channel later.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public enum InquiryCallbackStatus {
    NOT_READY,
    PENDING,
    PROCESSING,
    DELIVERED,
    FAILED
}
