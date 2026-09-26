package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.validation.Preconditions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Processes durable provider work and callback delivery without knowing any business consumer.
 * Provider calls and callbacks execute between independent transactional claim and completion phases.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultInquiryWorkerService implements InquiryWorkerService {

    private final InquiryWorkTransactionService transactions;
    private final DeferredInquiryService deferredInquiryService;
    private final InquiryCallbackDispatcher callbackDispatcher;

    @Override
    public AsyncInquiryModels.BatchResult processBatch(int batchSize) {
        Preconditions.requirePositive(batchSize, "batchSize");
        transactions.recoverStale();
        long processed = 0;
        long succeeded = 0;
        long failed = 0;
        boolean callbackTurn = true;
        while (processed < batchSize) {
            WorkOutcome outcome = callbackTurn ? processCallback() : processInquiry();
            if (outcome == WorkOutcome.NONE) {
                outcome = callbackTurn ? processInquiry() : processCallback();
            }
            if (outcome == WorkOutcome.NONE) {
                break;
            }
            processed++;
            if (outcome == WorkOutcome.SUCCEEDED) {
                succeeded++;
            } else {
                failed++;
            }
            callbackTurn = !callbackTurn;
        }
        return new AsyncInquiryModels.BatchResult(processed, succeeded, failed);
    }

    private WorkOutcome processInquiry() {
        Optional<AsyncInquiryModels.WorkItem> candidate = transactions.prepareNext();
        if (candidate.isEmpty()) {
            return WorkOutcome.NONE;
        }
        AsyncInquiryModels.WorkItem work = candidate.get();
        try {
            transactions.complete(work.requestId(), deferredInquiryService.execute(
                    new DeferredInquiryModels.Command(
                            work.inquiryCode(), work.nationalCode(), work.providerCode(),
                            work.externalTrackingCode()
                    )
            ));
            return WorkOutcome.SUCCEEDED;
        } catch (RuntimeException exception) {
            transactions.fail(work.requestId(), exception);
            return WorkOutcome.FAILED;
        }
    }

    private WorkOutcome processCallback() {
        Optional<AsyncInquiryModels.CallbackWork> candidate = transactions.prepareCallback();
        if (candidate.isEmpty()) {
            return WorkOutcome.NONE;
        }
        AsyncInquiryModels.CallbackWork work = candidate.get();
        try {
            callbackDispatcher.dispatch(work);
            transactions.callbackDelivered(work.event().requestId());
            return WorkOutcome.SUCCEEDED;
        } catch (RuntimeException exception) {
            transactions.callbackFailed(work.event().requestId(), exception);
            return WorkOutcome.FAILED;
        }
    }

    private enum WorkOutcome {
        NONE,
        SUCCEEDED,
        FAILED
    }
}
