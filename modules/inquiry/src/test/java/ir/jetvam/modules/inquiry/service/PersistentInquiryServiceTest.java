package ir.jetvam.modules.inquiry.service;

import tools.jackson.databind.ObjectMapper;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.integration.routing.ProviderExecution;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies durable synchronous execution and reuse of unexpired inquiry results.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
class PersistentInquiryServiceTest {

    private final RoutingInquiryService providerService = mock(RoutingInquiryService.class);
    private final InquiryExecutionStore executionStore = mock(InquiryExecutionStore.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PersistentInquiryService service = new PersistentInquiryService(
            providerService, executionStore, objectMapper
    );

    @Test
    void returnsValidPersistedResultWithoutCallingProvider() throws Exception {
        InquiryResults.CreditRating cached = new InquiryResults.CreditRating(
                "A2", 8, BigDecimal.valueOf(720), "cached-1"
        );
        UUID requestId = UUID.randomUUID();
        when(executionStore.prepare(
                org.mockito.ArgumentMatchers.eq(InquiryType.CREDIT_RATING),
                org.mockito.ArgumentMatchers.eq("0067749828"),
                org.mockito.ArgumentMatchers.eq("0067749828"),
                anyString()
        )).thenReturn(new InquiryExecutionStore.Preparation(
                requestId, objectMapper.writeValueAsString(cached), true, Duration.ofDays(7)
        ));

        InquiryResults.CreditRating result = service.findCreditRating(
                new InquiryRequests.CreditRating("۰۰۶۷۷۴۹۸۲۸")
        );

        assertThat(result).isEqualTo(cached);
        verify(providerService, never()).findCreditRating(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void persistsProviderResultWithItsValidity() {
        InquiryRequests.BadCheque normalized = new InquiryRequests.BadCheque("0067749828");
        InquiryResults.BadCheque providerResult = new InquiryResults.BadCheque(
                0, BigDecimal.ZERO, "provider-1"
        );
        UUID requestId = UUID.randomUUID();
        Duration validity = Duration.ofDays(1);
        when(executionStore.prepare(
                org.mockito.ArgumentMatchers.eq(InquiryType.BAD_CHEQUE),
                org.mockito.ArgumentMatchers.eq("0067749828"),
                org.mockito.ArgumentMatchers.eq("0067749828"),
                anyString()
        )).thenReturn(new InquiryExecutionStore.Preparation(requestId, null, false, validity));
        when(providerService.findBadCheques(normalized))
                .thenReturn(new ProviderExecution<>("CHEQUE_PROVIDER", providerResult));

        assertThat(service.findBadCheques(normalized)).isEqualTo(providerResult);

        verify(executionStore).complete(
                org.mockito.ArgumentMatchers.eq(requestId),
                org.mockito.ArgumentMatchers.eq("CHEQUE_PROVIDER"),
                org.mockito.ArgumentMatchers.eq("provider-1"),
                anyString(),
                org.mockito.ArgumentMatchers.eq(validity)
        );
    }
}
