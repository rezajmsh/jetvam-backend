package ir.jetvam.modules.inquiry.service;

import ir.jetvam.modules.inquiry.model.AsyncInquiryStatus;

import java.util.Map;
import java.util.UUID;

/**
 * Groups consumer-neutral asynchronous inquiry requests, callback configuration and outcomes.
 * Callback transport codes are extensible so MQ or webhook delivery can be added without changing consumers.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class AsyncInquiryModels {

    public static final String SPRING_BEAN = "SPRING_BEAN";

    private AsyncInquiryModels() {
    }

    public record Callback(String transport, String destination, String correlationId) {
    }

    public record Submit(String inquiryCode, String nationalCode, Callback callback) {
    }

    public record BatchResult(long processedCount, long succeededCount, long failedCount) {
    }

    public record WorkItem(
            UUID requestId,
            String inquiryCode,
            String nationalCode,
            String providerCode,
            String externalTrackingCode
    ) {
    }

    public record CompletionEvent(
            UUID requestId,
            String inquiryCode,
            AsyncInquiryStatus status,
            Map<String, String> facts,
            String rejectionCode,
            String message,
            String correlationId
    ) {
        public CompletionEvent {
            facts = facts == null ? Map.of() : Map.copyOf(facts);
        }
    }

    public record CallbackWork(Callback callback, CompletionEvent event) {
    }
}
