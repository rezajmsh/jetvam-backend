package ir.jetvam.apps.jobs.inquiry;

import ir.jetvam.apps.jobs.infrastructure.handler.JobContext;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandler;
import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.modules.inquiry.service.AsyncInquiryModels;
import ir.jetvam.modules.inquiry.service.InquiryExecutionWorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Runs provider submission and polling independently from callback delivery.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
@Component
@RequiredArgsConstructor
public class InquiryExecutionJobHandler implements JobHandler {

    public static final String KEY = "inquiry-execution";

    private final InquiryExecutionWorkerService workerService;
    private final InquiryJobItemRecorder itemRecorder;

    @Value("${jetvam.inquiry.execution-worker.batch-size:${jetvam.inquiry.worker.batch-size:50}}")
    private int batchSize;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public JobResult execute(JobContext context) {
        AsyncInquiryModels.BatchResult result = workerService.processBatch(batchSize);
        itemRecorder.record(context, result);
        return JobResult.completed(
                result.processedCount(), result.succeededCount(), result.failedCount(),
                "Inquiry provider execution batch completed"
        );
    }
}
