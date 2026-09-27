package ir.jetvam.modules.inquiry.model;

/**
 * Distinguishes request/response inquiries from background callback-based work.
 * Both modes share the same durable request and result history.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
public enum InquiryExecutionMode {
    SYNCHRONOUS,
    ASYNCHRONOUS
}
