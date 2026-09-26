package ir.jetvam.modules.inquiry.service;

/**
 * Describes whether an asynchronous inquiry needs polling or has reached a terminal outcome.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public enum DeferredInquiryStatus {
    PENDING,
    COMPLETED,
    REJECTED
}
