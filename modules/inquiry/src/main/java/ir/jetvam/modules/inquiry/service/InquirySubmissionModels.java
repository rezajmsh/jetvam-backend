package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.model.InquiryResponseMode;
import ir.jetvam.modules.inquiry.model.InquiryStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Unified request contract for persisted synchronous and asynchronous inquiries.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public final class InquirySubmissionModels {

    public static final String SPRING_BEAN = "SPRING_BEAN";

    private InquirySubmissionModels() {
    }

    public record Callback(String transport, String destination, String correlationId) {
    }

    public record Command(
            InquiryType inquiryType,
            String nationalCode,
            String subjectKey,
            Object request,
            InquiryResponseMode responseMode,
            Callback callback,
            Duration timeout
    ) {
        public static Command synchronous(
                InquiryType inquiryType,
                String nationalCode,
                String subjectKey,
                Object request
        ) {
            return new Command(
                    inquiryType, nationalCode, subjectKey, request,
                    InquiryResponseMode.SYNCHRONOUS, null, null
            );
        }

        public static Command asynchronous(
                InquiryType inquiryType,
                String nationalCode,
                String subjectKey,
                Object request,
                Callback callback
        ) {
            return new Command(
                    inquiryType, nationalCode, subjectKey, request,
                    InquiryResponseMode.ASYNC_CALLBACK, callback, null
            );
        }
    }

    public record Result(
            UUID requestId,
            InquiryStatus status,
            Map<String, String> facts,
            String providerCode,
            String rejectionCode,
            String message,
            boolean cacheHit,
            Instant nextAttemptAt
    ) {
        public Result {
            facts = facts == null ? Map.of() : Map.copyOf(facts);
        }

        public boolean terminal() {
            return status == InquiryStatus.COMPLETED
                    || status == InquiryStatus.REJECTED
                    || status == InquiryStatus.FAILED;
        }
    }
}
