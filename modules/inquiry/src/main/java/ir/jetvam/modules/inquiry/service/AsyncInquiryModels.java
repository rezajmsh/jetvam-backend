package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.execution.InquiryExecutionContext;
import ir.jetvam.modules.inquiry.model.InquiryResponseMode;
import ir.jetvam.modules.inquiry.model.InquiryStatus;

import java.util.List;
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

    public enum WorkKind {
        INQUIRY,
        CALLBACK
    }

    public record WorkResult(
            UUID requestId,
            WorkKind kind,
            InquiryType inquiryCode,
            String nationalCode,
            String subjectKey,
            boolean succeeded,
            String businessStatus,
            String providerCode,
            String externalReference,
            String message
    ) {
    }

    public record BatchResult(
            long processedCount,
            long succeededCount,
            long failedCount,
            List<WorkResult> items
    ) {
        public BatchResult {
            items = items == null ? List.of() : List.copyOf(items);
        }

        public BatchResult(long processedCount, long succeededCount, long failedCount) {
            this(processedCount, succeededCount, failedCount, List.of());
        }
    }

    public record WorkItem(
            UUID requestId,
            InquiryType inquiryCode,
            String nationalCode,
            String subjectKey,
            String requestJson,
            InquiryResponseMode responseMode,
            InquiryExecutionContext context
    ) {
    }

    public record CompletionEvent(
            UUID requestId,
            InquiryType inquiryCode,
            String nationalCode,
            String subjectKey,
            String providerCode,
            InquiryStatus status,
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
