package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.modules.inquiry.service.DeferredInquiryStatus;

import java.math.BigDecimal;

/**
 * Defines the canonical submit and poll protocol used by asynchronous credit-rating adapters.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class CreditRatingProtocol {

    private CreditRatingProtocol() {
    }

    public record Submit(String nationalCode) {
    }

    public record Poll(String trackingCode) {
    }

    public record Progress(
            DeferredInquiryStatus status,
            String trackingCode,
            int retryAfterSeconds,
            String ratingCode,
            Integer rank,
            BigDecimal score,
            String rejectionCode,
            String rejectionMessage
    ) {
    }
}
