package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.model.InquiryResponseMode;
import ir.jetvam.modules.inquiry.model.InquiryStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/**
 * Verifies durable synchronous execution and reuse of unexpired inquiry results.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
class PersistentInquiryServiceTest {

    private final InquiryRequestService requestService = mock(InquiryRequestService.class);
    private final PersistentInquiryService service = new PersistentInquiryService(requestService);

    @Test
    void submitsSynchronousCommandAndMapsPersistedFacts() {
        UUID requestId = UUID.randomUUID();
        when(requestService.submit(any())).thenReturn(new InquirySubmissionModels.Result(
                requestId, InquiryStatus.COMPLETED,
                Map.of("ratingCode", "A2", "rank", "8", "score", "720", "trackingId", "cached-1"),
                "CREDIT_BUREAU", null, null, true, null
        ));

        InquiryResults.CreditRating result = service.findCreditRating(
                new InquiryRequests.CreditRating("۰۰۶۷۷۴۹۸۲۸")
        );

        assertThat(result).isEqualTo(new InquiryResults.CreditRating(
                "A2", 8, BigDecimal.valueOf(720), "cached-1"
        ));
        var captor = org.mockito.ArgumentCaptor.forClass(InquirySubmissionModels.Command.class);
        verify(requestService).submit(captor.capture());
        assertThat(captor.getValue().inquiryType()).isEqualTo(InquiryType.CREDIT_RATING);
        assertThat(captor.getValue().responseMode()).isEqualTo(InquiryResponseMode.SYNCHRONOUS);
        assertThat(captor.getValue().nationalCode()).isEqualTo("0067749828");
    }

    @Test
    void usesMobileInSubjectKeyToKeepShahkarReuseSafe() {
        when(requestService.submit(any())).thenReturn(new InquirySubmissionModels.Result(
                UUID.randomUUID(), InquiryStatus.COMPLETED, Map.of("matched", "true", "trackingId", "track-1"),
                "SHAHKAR", null, null, false, null
        ));

        service.verifyMobileOwnership(new InquiryRequests.MobileOwnership("09121234567", "0067749828"));

        var captor = org.mockito.ArgumentCaptor.forClass(InquirySubmissionModels.Command.class);
        verify(requestService).submit(captor.capture());
        assertThat(captor.getValue().subjectKey()).isEqualTo("0067749828:09121234567");
    }
}
