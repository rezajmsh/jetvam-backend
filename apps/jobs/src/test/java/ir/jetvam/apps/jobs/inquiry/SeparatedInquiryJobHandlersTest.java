package ir.jetvam.apps.jobs.inquiry;

import ir.jetvam.apps.jobs.infrastructure.handler.JobContext;
import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;
import ir.jetvam.modules.inquiry.service.AsyncInquiryModels;
import ir.jetvam.modules.inquiry.service.InquiryCallbackWorkerService;
import ir.jetvam.modules.inquiry.service.InquiryExecutionWorkerService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that provider execution and callback delivery are independently scheduled handlers.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
class SeparatedInquiryJobHandlersTest {

    @Test
    void executionHandlerRunsOnlyExecutionWorker() {
        InquiryExecutionWorkerService worker = mock(InquiryExecutionWorkerService.class);
        InquiryJobItemRecorder recorder = mock(InquiryJobItemRecorder.class);
        AsyncInquiryModels.BatchResult batch = new AsyncInquiryModels.BatchResult(3, 2, 1);
        when(worker.processBatch(20)).thenReturn(batch);
        InquiryExecutionJobHandler handler = new InquiryExecutionJobHandler(worker, recorder);
        ReflectionTestUtils.setField(handler, "batchSize", 20);
        JobContext context = context(InquiryExecutionJobHandler.KEY);

        JobResult result = handler.execute(context);

        assertThat(handler.key()).isEqualTo("inquiry-execution");
        assertThat(result.processedCount()).isEqualTo(3);
        verify(worker).processBatch(20);
        verify(recorder).record(context, batch);
    }

    @Test
    void callbackHandlerRunsOnlyCallbackWorker() {
        InquiryCallbackWorkerService worker = mock(InquiryCallbackWorkerService.class);
        InquiryJobItemRecorder recorder = mock(InquiryJobItemRecorder.class);
        AsyncInquiryModels.BatchResult batch = new AsyncInquiryModels.BatchResult(4, 4, 0);
        when(worker.processBatch(15)).thenReturn(batch);
        InquiryCallbackJobHandler handler = new InquiryCallbackJobHandler(worker, recorder);
        ReflectionTestUtils.setField(handler, "batchSize", 15);
        JobContext context = context(InquiryCallbackJobHandler.KEY);

        JobResult result = handler.execute(context);

        assertThat(handler.key()).isEqualTo("inquiry-callback");
        assertThat(result.succeededCount()).isEqualTo(4);
        verify(worker).processBatch(15);
        verify(recorder).record(context, batch);
    }

    private static JobContext context(String jobCode) {
        Instant now = Instant.now();
        return new JobContext(
                UUID.randomUUID(), UUID.randomUUID(), jobCode, JobTriggerType.MANUAL, "admin", now, now
        );
    }
}
