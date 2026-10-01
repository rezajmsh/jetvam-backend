package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.validation.Preconditions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Processes only provider execution and polling work from the durable inquiry queue.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultInquiryExecutionWorkerService implements InquiryExecutionWorkerService {

    private final InquiryWorkTransactionService transactions;
    private final InquiryRequestExecutor requestExecutor;

    @Override
    public AsyncInquiryModels.BatchResult processBatch(int batchSize) {
        Preconditions.requirePositive(batchSize, "batchSize");
        transactions.recoverStaleExecutions();
        List<AsyncInquiryModels.WorkResult> items = new ArrayList<>();
        while (items.size() < batchSize) {
            Optional<AsyncInquiryModels.WorkItem> candidate = transactions.prepareNext();
            if (candidate.isEmpty()) {
                break;
            }
            items.add(requestExecutor.execute(candidate.get()));
        }
        long succeeded = items.stream().filter(AsyncInquiryModels.WorkResult::succeeded).count();
        return new AsyncInquiryModels.BatchResult(items.size(), succeeded, items.size() - succeeded, items);
    }

}
