package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.validation.Preconditions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Processes only callback delivery work after an inquiry reaches a terminal provider outcome.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultInquiryCallbackWorkerService implements InquiryCallbackWorkerService {

    private static final Logger LOGGER = LoggerFactory.getLogger("jetvam.inquiry.callback-worker");

    private final InquiryWorkTransactionService transactions;
    private final InquiryCallbackDispatcher callbackDispatcher;

    @Override
    public AsyncInquiryModels.BatchResult processBatch(int batchSize) {
        Preconditions.requirePositive(batchSize, "batchSize");
        transactions.recoverStaleCallbacks();
        List<AsyncInquiryModels.WorkResult> items = new ArrayList<>();
        while (items.size() < batchSize) {
            Optional<AsyncInquiryModels.CallbackWork> candidate = transactions.prepareCallback();
            if (candidate.isEmpty()) {
                break;
            }
            items.add(process(candidate.get()));
        }
        long succeeded = items.stream().filter(AsyncInquiryModels.WorkResult::succeeded).count();
        return new AsyncInquiryModels.BatchResult(items.size(), succeeded, items.size() - succeeded, items);
    }

    private AsyncInquiryModels.WorkResult process(AsyncInquiryModels.CallbackWork work) {
        LOGGER.info(
                "inquiry.callback.started requestId={} inquiryCode={} transport={} destination={}",
                work.event().requestId(), work.event().inquiryCode(), work.callback().transport(),
                work.callback().destination()
        );
        try {
            callbackDispatcher.dispatch(work);
            transactions.callbackDelivered(work.event().requestId());
            LOGGER.info(
                    "inquiry.callback.completed requestId={} inquiryCode={} destination={}",
                    work.event().requestId(), work.event().inquiryCode(), work.callback().destination()
            );
            return result(work, true, "DELIVERED", null);
        } catch (RuntimeException exception) {
            transactions.callbackFailed(work.event().requestId(), exception);
            LOGGER.warn(
                    "inquiry.callback.failed requestId={} inquiryCode={} destination={} errorType={} errorMessage={}",
                    work.event().requestId(), work.event().inquiryCode(), work.callback().destination(),
                    exception.getClass().getSimpleName(), exception.getMessage()
            );
            return result(work, false, "DELIVERY_FAILED", message(exception));
        }
    }

    private static AsyncInquiryModels.WorkResult result(
            AsyncInquiryModels.CallbackWork work,
            boolean succeeded,
            String status,
            String message
    ) {
        AsyncInquiryModels.CompletionEvent event = work.event();
        return new AsyncInquiryModels.WorkResult(
                event.requestId(), AsyncInquiryModels.WorkKind.CALLBACK, event.inquiryCode(),
                event.nationalCode(), event.subjectKey(), succeeded, status,
                event.providerCode(), event.facts().get("trackingId"), message
        );
    }

    private static String message(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
