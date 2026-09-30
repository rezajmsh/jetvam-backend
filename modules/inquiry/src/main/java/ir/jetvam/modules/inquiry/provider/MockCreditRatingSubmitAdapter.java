package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.service.DeferredInquiryStatus;
import ir.jetvam.modules.integration.routing.ExternalProviderAdapter;
import ir.jetvam.modules.integration.routing.ProviderInvocationContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Starts the mock credit-rating protocol and returns a provider tracking code.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Component
public final class MockCreditRatingSubmitAdapter implements ExternalProviderAdapter<
        CreditRatingProtocol.Submit,
        CreditRatingProtocol.Progress> {

    public static final String ADAPTER_CODE = "CREDIT_RATING_SUBMIT_MOCK_V1";

    @Override public String capabilityCode() { return InquiryType.CREDIT_RATING_SUBMIT.code(); }
    @Override public String adapterCode() { return ADAPTER_CODE; }
    @Override public Class<CreditRatingProtocol.Submit> commandType() { return CreditRatingProtocol.Submit.class; }

    @Override
    public CreditRatingProtocol.Progress execute(
            CreditRatingProtocol.Submit command,
            ProviderInvocationContext context
    ) {
        Preconditions.requireNonNull(command, "command");
        return new CreditRatingProtocol.Progress(
                DeferredInquiryStatus.PENDING,
                "MOCK-CREDIT-" + UUID.randomUUID(),
                30,
                null,
                null,
                null,
                null,
                null
        );
    }
}
