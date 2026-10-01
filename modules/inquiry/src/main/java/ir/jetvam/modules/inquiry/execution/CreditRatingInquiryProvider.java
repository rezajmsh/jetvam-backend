package ir.jetvam.modules.inquiry.execution;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.provider.CreditRatingProgressStatus;
import ir.jetvam.modules.inquiry.provider.CreditRatingProtocol;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.integration.routing.ProviderExecution;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Owns the submit/poll protocol and provider affinity of the credit-rating inquiry.
 * Generic request execution treats the returned context as opaque durable state.
 *
 * @author reza jamshidi
 * @since 10/1/2026
 */
@Component
@RequiredArgsConstructor
public class CreditRatingInquiryProvider implements TypedInquiryProvider<InquiryRequests.CreditRating> {

    private static final String STAGE = "stage";
    private static final String POLL_STAGE = "POLL";
    private static final String TRACKING_CODE = "trackingCode";

    private final ProviderRouter providerRouter;

    @Override
    public InquiryType inquiryType() {
        return InquiryType.CREDIT_RATING;
    }

    @Override
    public Class<InquiryRequests.CreditRating> requestType() {
        return InquiryRequests.CreditRating.class;
    }

    @Override
    public InquiryExecutionResult execute(
            String nationalCode,
            InquiryRequests.CreditRating request,
            InquiryExecutionContext context
    ) {
        ExecutionProgress execution = context.data().isEmpty()
                ? submit(nationalCode)
                : continueExecution(context);
        CreditRatingProtocol.Progress progress = execution.progress();
        String trackingCode = trackingCode(progress, context);
        if (progress.status() == CreditRatingProgressStatus.PENDING) {
            InquiryExecutionContext nextContext = pollingContext(
                    execution.providerCode(),
                    Preconditions.requireText(trackingCode, "provider.trackingCode")
            );
            return InquiryExecutionResult.pending(
                    nextContext,
                    trackingCode,
                    Math.max(30, progress.retryAfterSeconds())
            );
        }
        InquiryExecutionContext terminalContext = trackingCode == null
                ? new InquiryExecutionContext(execution.providerCode(), Map.of())
                : pollingContext(execution.providerCode(), trackingCode);
        if (progress.status() == CreditRatingProgressStatus.REJECTED) {
            return new InquiryExecutionResult(
                    InquiryExecutionStatus.REJECTED,
                    terminalContext,
                    trackingCode,
                    0,
                    Map.of(),
                    progress.rejectionCode(),
                    progress.rejectionMessage()
            );
        }
        Preconditions.requireNonNull(progress.rank(), "provider.rank");
        Map<String, String> facts = new LinkedHashMap<>();
        put(facts, "ratingCode", progress.ratingCode());
        facts.put("rank", progress.rank().toString());
        if (progress.score() != null) {
            facts.put("score", progress.score().toPlainString());
        }
        put(facts, "trackingId", trackingCode);
        return InquiryExecutionResult.completed(terminalContext, trackingCode, facts);
    }

    private ExecutionProgress submit(String nationalCode) {
        ProviderExecution<CreditRatingProtocol.Progress> execution = providerRouter.executeWithProvider(
                InquiryType.CREDIT_RATING_SUBMIT.code(),
                new CreditRatingProtocol.Submit(nationalCode),
                CreditRatingProtocol.Progress.class,
                true
        );
        return new ExecutionProgress(execution.providerCode(), execution.result());
    }

    private ExecutionProgress continueExecution(InquiryExecutionContext context) {
        String stage = Preconditions.requireText(context.value(STAGE), "context.stage");
        if (!POLL_STAGE.equals(stage)) {
            throw new IllegalArgumentException("Unsupported credit-rating stage: " + stage);
        }
        String providerCode = Preconditions.requireText(context.providerCode(), "context.providerCode");
        String trackingCode = Preconditions.requireText(context.value(TRACKING_CODE), "context.trackingCode");
        CreditRatingProtocol.Progress progress = providerRouter.executeOnProvider(
                InquiryType.CREDIT_RATING_POLL.code(),
                providerCode,
                new CreditRatingProtocol.Poll(trackingCode),
                CreditRatingProtocol.Progress.class
        );
        return new ExecutionProgress(providerCode, progress);
    }

    private static InquiryExecutionContext pollingContext(String providerCode, String trackingCode) {
        return new InquiryExecutionContext(
                Preconditions.requireText(providerCode, "providerCode"),
                Map.of(
                        STAGE, POLL_STAGE,
                        TRACKING_CODE, Preconditions.requireText(trackingCode, "provider.trackingCode")
                )
        );
    }

    private static String trackingCode(
            CreditRatingProtocol.Progress progress,
            InquiryExecutionContext currentContext
    ) {
        if (progress.trackingCode() != null && !progress.trackingCode().isBlank()) {
            return progress.trackingCode();
        }
        return currentContext.value(TRACKING_CODE);
    }

    private static void put(Map<String, String> facts, String key, String value) {
        if (value != null && !value.isBlank()) {
            facts.put(key, value);
        }
    }

    private record ExecutionProgress(String providerCode, CreditRatingProtocol.Progress progress) {
    }
}
