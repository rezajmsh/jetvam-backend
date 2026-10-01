package ir.jetvam.apps.jobs.inquiry;

import ir.jetvam.apps.jobs.infrastructure.handler.JobContext;
import ir.jetvam.apps.jobs.infrastructure.model.JobExecutionItemStatus;
import ir.jetvam.apps.jobs.infrastructure.service.JobExecutionItemCommand;
import ir.jetvam.apps.jobs.infrastructure.service.JobExecutionItemPersistenceService;
import ir.jetvam.modules.inquiry.service.AsyncInquiryModels;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Maps inquiry-module work results to generic item-level job execution history.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
@Component
@RequiredArgsConstructor
public class InquiryJobItemRecorder {

    private final JobExecutionItemPersistenceService persistenceService;

    public void record(JobContext context, AsyncInquiryModels.BatchResult result) {
        persistenceService.record(context.executionId(), result.items().stream()
                .map(item -> new JobExecutionItemCommand(
                        item.kind().name(), item.requestId().toString(), item.inquiryCode().code(),
                        item.nationalCode(), item.subjectKey(),
                        item.succeeded() ? JobExecutionItemStatus.SUCCEEDED : JobExecutionItemStatus.FAILED,
                        item.businessStatus(), item.providerCode(), item.externalReference(), item.message()
                ))
                .toList());
    }
}
