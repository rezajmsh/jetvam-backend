package ir.jetvam.modules.inquiry.model;

/**
 * Tracks provider execution independently from execution mode and consumer.
 * Asynchronous terminal outcomes are additionally delivered through a callback transport.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
public enum InquiryStatus {
    QUEUED,
    PROCESSING,
    WAITING_PROVIDER,
    COMPLETED,
    REJECTED,
    FAILED
}
