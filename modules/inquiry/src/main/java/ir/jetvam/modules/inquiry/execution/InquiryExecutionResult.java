package ir.jetvam.modules.inquiry.execution;

import java.util.Map;

/**
 * Normalized provider result persisted identically for synchronous and asynchronous consumers.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public record InquiryExecutionResult(
        InquiryExecutionStatus status,
        InquiryExecutionContext context,
        String externalReference,
        int retryAfterSeconds,
        Map<String, String> facts,
        String rejectionCode,
        String rejectionMessage
) {
    public InquiryExecutionResult {
        context = context == null ? InquiryExecutionContext.empty() : context;
        facts = facts == null ? Map.of() : Map.copyOf(facts);
    }

    public static InquiryExecutionResult completed(
            InquiryExecutionContext context,
            String externalReference,
            Map<String, String> facts
    ) {
        return new InquiryExecutionResult(
                InquiryExecutionStatus.COMPLETED, context, externalReference, 0, facts, null, null
        );
    }

    public static InquiryExecutionResult pending(
            InquiryExecutionContext context,
            String externalReference,
            int retryAfterSeconds
    ) {
        return new InquiryExecutionResult(
                InquiryExecutionStatus.PENDING, context, externalReference, retryAfterSeconds,
                Map.of(), null, null
        );
    }
}
