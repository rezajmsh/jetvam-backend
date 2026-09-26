package ir.jetvam.apps.jobs.inquiry;

import ir.jetvam.apps.jobs.infrastructure.handler.JobContext;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandler;
import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.modules.inquiry.service.AsyncInquiryModels;
import ir.jetvam.modules.inquiry.service.InquiryWorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Delegates scheduled provider and callback work to the independent Inquiry module.
 * It contains no knowledge of Origination or any other inquiry consumer.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
@RequiredArgsConstructor
public class InquiryDispatchJobHandler implements JobHandler {

    public static final String KEY = "inquiry-dispatch";

    private final InquiryWorkerService inquiryWorkerService;

    @Value("${jetvam.inquiry.worker.batch-size:50}")
    private int batchSize;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public JobResult execute(JobContext context) {
        AsyncInquiryModels.BatchResult result = inquiryWorkerService.processBatch(batchSize);
        return JobResult.completed(
                result.processedCount(), result.succeededCount(), result.failedCount(),
                "Inquiry provider and callback batch completed"
        );
    }
}
