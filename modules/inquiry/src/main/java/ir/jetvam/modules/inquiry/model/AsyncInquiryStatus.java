package ir.jetvam.modules.inquiry.model;

/**
 * Tracks provider execution independently from every inquiry consumer.
 * Terminal outcomes are delivered through the configured callback transport.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public enum AsyncInquiryStatus {
    QUEUED,
    PROCESSING,
    WAITING_PROVIDER,
    COMPLETED,
    REJECTED,
    FAILED
}
