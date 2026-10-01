package ir.jetvam.modules.inquiry.model;

/**
 * Defines how the consumer receives a persisted inquiry result.
 * Provider execution is identical for both modes; only request coordination and delivery differ.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public enum InquiryResponseMode {
    SYNCHRONOUS,
    ASYNC_CALLBACK
}
