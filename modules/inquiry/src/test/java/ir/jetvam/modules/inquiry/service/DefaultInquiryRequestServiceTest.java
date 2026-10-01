package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.execution.InquiryExecutionContext;
import ir.jetvam.modules.inquiry.model.InquiryResponseMode;
import ir.jetvam.modules.inquiry.model.InquiryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the response mode changes coordination only, not registration or execution boundaries.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
class DefaultInquiryRequestServiceTest {

    private final InquiryRequestRegistrationService registrations = mock(InquiryRequestRegistrationService.class);
    private final InquiryWorkTransactionService transactions = mock(InquiryWorkTransactionService.class);
    private final InquiryRequestExecutor executor = mock(InquiryRequestExecutor.class);
    private final DefaultInquiryRequestService service = service();

    @Test
    void asynchronousModeOnlyRegistersDurableRequest() {
        InquirySubmissionModels.Command command = asyncCommand();
        InquirySubmissionModels.Result queued = result(InquiryStatus.QUEUED, Map.of());
        when(registrations.register(command)).thenReturn(queued);

        assertThat(service.submit(command)).isEqualTo(queued);

        verify(transactions, never()).prepare(queued.requestId());
        verify(executor, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void synchronousModeClaimsAndUsesSameExecutorAsJob() {
        InquirySubmissionModels.Command command = InquirySubmissionModels.Command.synchronous(
                InquiryType.BAD_CHEQUE, "0067749828", "0067749828",
                new InquiryRequests.BadCheque("0067749828")
        );
        InquirySubmissionModels.Result queued = result(InquiryStatus.QUEUED, Map.of());
        InquirySubmissionModels.Result completed = result(
                InquiryStatus.COMPLETED, Map.of("unsettledCount", "0", "totalAmount", "0")
        );
        AsyncInquiryModels.WorkItem work = new AsyncInquiryModels.WorkItem(
                queued.requestId(), InquiryType.BAD_CHEQUE, "0067749828", "0067749828",
                "{}", InquiryResponseMode.SYNCHRONOUS, InquiryExecutionContext.empty()
        );
        when(registrations.register(command)).thenReturn(queued);
        when(transactions.prepare(queued.requestId())).thenReturn(Optional.of(work));
        when(executor.execute(work)).thenReturn(new AsyncInquiryModels.WorkResult(
                queued.requestId(), AsyncInquiryModels.WorkKind.INQUIRY, InquiryType.BAD_CHEQUE,
                "0067749828", "0067749828", true, "COMPLETED", "MOCK", "track-1", null
        ));
        when(registrations.find(queued.requestId())).thenReturn(completed);

        assertThat(service.submit(command).status()).isEqualTo(InquiryStatus.COMPLETED);

        verify(executor).execute(work);
    }

    private DefaultInquiryRequestService service() {
        DefaultInquiryRequestService result = new DefaultInquiryRequestService(registrations, transactions, executor);
        ReflectionTestUtils.setField(result, "defaultTimeout", Duration.ofSeconds(1));
        return result;
    }

    private static InquirySubmissionModels.Command asyncCommand() {
        return InquirySubmissionModels.Command.asynchronous(
                InquiryType.BAD_CHEQUE, "0067749828", "0067749828",
                new InquiryRequests.BadCheque("0067749828"),
                new InquirySubmissionModels.Callback("SPRING_BEAN", "origination", "control-1")
        );
    }

    private static InquirySubmissionModels.Result result(InquiryStatus status, Map<String, String> facts) {
        return new InquirySubmissionModels.Result(
                UUID.fromString("12c4c364-b796-4c14-a97e-0e3a00b681c8"), status, facts,
                status == InquiryStatus.COMPLETED ? "MOCK" : null,
                null, null, false, null
        );
    }
}
