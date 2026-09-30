package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.integration.routing.ExternalProviderAdapter;
import ir.jetvam.modules.integration.routing.ProviderInvocationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Supports direct credit-rating reads while workflow execution uses submit and poll adapters.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Component
public final class MockCreditRatingAdapter implements ExternalProviderAdapter<InquiryRequests.CreditRating, InquiryResults.CreditRating> {

    public static final String ADAPTER_CODE = "CREDIT_RATING_MOCK_V1";

    @Override public String capabilityCode() { return InquiryType.CREDIT_RATING.code(); }
    @Override public String adapterCode() { return ADAPTER_CODE; }
    @Override public Class<InquiryRequests.CreditRating> commandType() { return InquiryRequests.CreditRating.class; }

    @Override
    public InquiryResults.CreditRating execute(InquiryRequests.CreditRating command, ProviderInvocationContext context) {
        Preconditions.requireNonNull(command, "command");
        return new InquiryResults.CreditRating(
                "A2", 8, BigDecimal.valueOf(720), "MOCK-CREDIT-DIRECT-" + UUID.randomUUID()
        );
    }
}
