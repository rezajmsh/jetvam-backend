package ir.jetvam.modules.inquiry.service;

import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import ir.jetvam.modules.integration.routing.ProviderExecution;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies capability routing and identifier normalization at the Inquiry boundary.
 * The tests keep provider selection details outside business consumers.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class RoutingInquiryServiceTest {

    @Test
    void routesCreditRatingThroughItsCanonicalCapability() {
        ProviderRouter router = mock(ProviderRouter.class);
        InquiryResults.CreditRating expected = new InquiryResults.CreditRating(
                "A2", 8, BigDecimal.valueOf(720), "rating-1"
        );
        InquiryRequests.CreditRating normalized = new InquiryRequests.CreditRating("0067749828");
        when(router.executeWithProvider(
                InquiryCapabilities.CREDIT_RATING, normalized, InquiryResults.CreditRating.class, true
        )).thenReturn(new ProviderExecution<>("BUREAU", expected));
        RoutingInquiryService service = new RoutingInquiryService(router);

        ProviderExecution<InquiryResults.CreditRating> result = service.findCreditRating(normalized);

        assertThat(result.result()).isEqualTo(expected);
        verify(router).executeWithProvider(
                InquiryCapabilities.CREDIT_RATING, normalized, InquiryResults.CreditRating.class, true
        );
    }

    @Test
    void routesBadChequeThroughItsCanonicalCapability() {
        ProviderRouter router = mock(ProviderRouter.class);
        InquiryRequests.BadCheque request = new InquiryRequests.BadCheque("0067749828");
        InquiryResults.BadCheque expected = new InquiryResults.BadCheque(0, BigDecimal.ZERO, "cheque-1");
        when(router.executeWithProvider(
                InquiryCapabilities.BAD_CHEQUE, request, InquiryResults.BadCheque.class, true
        )).thenReturn(new ProviderExecution<>("BUREAU", expected));
        RoutingInquiryService service = new RoutingInquiryService(router);

        assertThat(service.findBadCheques(request).result()).isEqualTo(expected);
        verify(router).executeWithProvider(
                InquiryCapabilities.BAD_CHEQUE, request, InquiryResults.BadCheque.class, true
        );
    }
}
