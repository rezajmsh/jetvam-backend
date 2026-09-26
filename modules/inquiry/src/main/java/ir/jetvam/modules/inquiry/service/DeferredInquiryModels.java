package ir.jetvam.modules.inquiry.service;

import java.util.Map;

/**
 * Groups provider-neutral commands and outcomes used by asynchronous inquiry workers.
 * Provider identity and tracking data are retained for safe polling of multi-step APIs.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class DeferredInquiryModels {

    private DeferredInquiryModels() {
    }

    public record Command(
            String inquiryCode,
            String nationalCode,
            String providerCode,
            String externalTrackingCode
    ) {
        public boolean polling() {
            return externalTrackingCode != null && !externalTrackingCode.isBlank();
        }
    }

    public record Result(
            DeferredInquiryStatus status,
            String providerCode,
            String externalTrackingCode,
            int retryAfterSeconds,
            Map<String, String> facts,
            String rejectionCode,
            String rejectionMessage
    ) {
        public Result {
            facts = facts == null ? Map.of() : Map.copyOf(facts);
        }

        public static Result completed(String providerCode, String trackingCode, Map<String, String> facts) {
            return new Result(
                    DeferredInquiryStatus.COMPLETED, providerCode, trackingCode, 0, facts, null, null
            );
        }

        public static Result pending(String providerCode, String trackingCode, int retryAfterSeconds) {
            return new Result(
                    DeferredInquiryStatus.PENDING, providerCode, trackingCode, retryAfterSeconds,
                    Map.of(), null, null
            );
        }
    }
}
