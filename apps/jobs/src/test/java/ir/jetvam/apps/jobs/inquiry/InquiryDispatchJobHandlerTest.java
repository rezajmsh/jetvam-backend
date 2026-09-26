package ir.jetvam.apps.jobs.inquiry;

import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.modules.inquiry.service.AsyncInquiryModels;
import ir.jetvam.modules.inquiry.service.InquiryWorkerService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the generic Inquiry job maps module counters to execution history.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class InquiryDispatchJobHandlerTest {

    @Test
    void mapsModuleBatchCountersToJobExecutionHistory() {
        InquiryWorkerService service = mock(InquiryWorkerService.class);
        when(service.processBatch(25)).thenReturn(new AsyncInquiryModels.BatchResult(4, 3, 1));
        InquiryDispatchJobHandler handler = new InquiryDispatchJobHandler(service);
        ReflectionTestUtils.setField(handler, "batchSize", 25);

        JobResult result = handler.execute(null);

        assertThat(result.processedCount()).isEqualTo(4);
        assertThat(result.succeededCount()).isEqualTo(3);
        assertThat(result.failedCount()).isEqualTo(1);
    }
}
