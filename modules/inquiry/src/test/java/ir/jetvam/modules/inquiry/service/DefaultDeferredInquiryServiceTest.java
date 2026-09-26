package ir.jetvam.modules.inquiry.service;

import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.provider.CreditRatingProtocol;
import ir.jetvam.modules.integration.routing.ProviderExecution;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultDeferredInquiryServiceTest {

    @Test
    void persistsProviderAffinityAcrossSubmitAndPollSteps() {
        ProviderRouter router = mock(ProviderRouter.class);
        InquiryService synchronous = mock(InquiryService.class);
        CreditRatingProtocol.Progress pending = new CreditRatingProtocol.Progress(
                DeferredInquiryStatus.PENDING, "tracking-1", 120, null, null, null, null, null
        );
        when(router.executeWithProvider(
                InquiryCapabilities.CREDIT_RATING_SUBMIT,
                new CreditRatingProtocol.Submit("0067749828"),
                CreditRatingProtocol.Progress.class,
                true
        )).thenReturn(new ProviderExecution<>("BUREAU_A", pending));
        CreditRatingProtocol.Progress completed = new CreditRatingProtocol.Progress(
                DeferredInquiryStatus.COMPLETED, "tracking-1", 0, "A2", 8,
                BigDecimal.valueOf(720), null, null
        );
        when(router.executeOnProvider(
                InquiryCapabilities.CREDIT_RATING_POLL,
                "BUREAU_A",
                new CreditRatingProtocol.Poll("tracking-1"),
                CreditRatingProtocol.Progress.class
        )).thenReturn(completed);
        DefaultDeferredInquiryService service = new DefaultDeferredInquiryService(synchronous, router);

        DeferredInquiryModels.Result submitted = service.execute(new DeferredInquiryModels.Command(
                InquiryCapabilities.CREDIT_RATING, "0067749828", null, null
        ));
        DeferredInquiryModels.Result polled = service.execute(new DeferredInquiryModels.Command(
                InquiryCapabilities.CREDIT_RATING, "0067749828", submitted.providerCode(),
                submitted.externalTrackingCode()
        ));

        assertThat(submitted.status()).isEqualTo(DeferredInquiryStatus.PENDING);
        assertThat(polled.status()).isEqualTo(DeferredInquiryStatus.COMPLETED);
        assertThat(polled.facts()).containsEntry("rank", "8");
        verify(router).executeOnProvider(
                InquiryCapabilities.CREDIT_RATING_POLL,
                "BUREAU_A",
                new CreditRatingProtocol.Poll("tracking-1"),
                CreditRatingProtocol.Progress.class
        );
    }
}
