package ir.jetvam.modules.inquiry.execution;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.provider.CreditRatingProgressStatus;
import ir.jetvam.modules.inquiry.provider.CreditRatingProtocol;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.integration.routing.ProviderExecution;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the typed credit-rating provider owns its stages and provider affinity.
 *
 * @author reza jamshidi
 * @since 10/1/2026
 */
class CreditRatingInquiryProviderTest {

    @Test
    void persistsExplicitPollStageInOpaqueExecutionContext() {
        ProviderRouter router = mock(ProviderRouter.class);
        CreditRatingProtocol.Progress pending = new CreditRatingProtocol.Progress(
                CreditRatingProgressStatus.PENDING, "tracking-1", 120, null, null, null, null, null
        );
        when(router.executeWithProvider(
                InquiryType.CREDIT_RATING_SUBMIT.code(),
                new CreditRatingProtocol.Submit("0067749828"),
                CreditRatingProtocol.Progress.class,
                true
        )).thenReturn(new ProviderExecution<>("BUREAU_A", pending));
        CreditRatingProtocol.Progress completed = new CreditRatingProtocol.Progress(
                CreditRatingProgressStatus.COMPLETED, "tracking-1", 0, "A2", 8,
                BigDecimal.valueOf(720), null, null
        );
        when(router.executeOnProvider(
                InquiryType.CREDIT_RATING_POLL.code(),
                "BUREAU_A",
                new CreditRatingProtocol.Poll("tracking-1"),
                CreditRatingProtocol.Progress.class
        )).thenReturn(completed);
        CreditRatingInquiryProvider provider = new CreditRatingInquiryProvider(router);

        InquiryExecutionResult submitted = provider.execute(
                "0067749828",
                new InquiryRequests.CreditRating("0067749828"),
                InquiryExecutionContext.empty()
        );
        InquiryExecutionResult polled = provider.execute(
                "0067749828",
                new InquiryRequests.CreditRating("0067749828"),
                submitted.context()
        );

        assertThat(submitted.status()).isEqualTo(InquiryExecutionStatus.PENDING);
        assertThat(submitted.context().data())
                .containsEntry("stage", "POLL")
                .containsEntry("trackingCode", "tracking-1");
        assertThat(polled.status()).isEqualTo(InquiryExecutionStatus.COMPLETED);
        assertThat(polled.facts()).containsEntry("rank", "8");
        verify(router).executeOnProvider(
                InquiryType.CREDIT_RATING_POLL.code(), "BUREAU_A",
                new CreditRatingProtocol.Poll("tracking-1"), CreditRatingProtocol.Progress.class
        );
    }
}
