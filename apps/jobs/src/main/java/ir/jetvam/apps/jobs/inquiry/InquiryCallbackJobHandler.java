package ir.jetvam.apps.jobs.inquiry;

import ir.jetvam.apps.jobs.infrastructure.handler.JobContext;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandler;
import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.modules.inquiry.service.AsyncInquiryModels;
import ir.jetvam.modules.inquiry.service.InquiryCallbackWorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Delivers completed inquiry callbacks as a separately managed operational job.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
@Component
@RequiredArgsConstructor
public class InquiryCallbackJobHandler implements JobHandler {

    public static final String KEY = "inquiry-callback";

    private final InquiryCallbackWorkerService workerService;
    private final InquiryJobItemRecorder itemRecorder;

    @Value("${jetvam.inquiry.callback-worker.batch-size:${jetvam.inquiry.worker.batch-size:50}}")
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
                "Inquiry callback delivery batch completed"
        );
    }
}
