package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.integration.routing.ExternalProviderAdapter;
import ir.jetvam.modules.integration.routing.ProviderInvocationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Completes the second step of the mock credit-rating protocol.
 * Provider affinity is preserved by using the same provider code as the submit adapter.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Component
public final class MockCreditRatingPollAdapter implements ExternalProviderAdapter<
        CreditRatingProtocol.Poll,
        CreditRatingProtocol.Progress> {

    public static final String ADAPTER_CODE = "CREDIT_RATING_POLL_MOCK_V1";

    @Override public String capabilityCode() { return InquiryType.CREDIT_RATING_POLL.code(); }
    @Override public String adapterCode() { return ADAPTER_CODE; }
    @Override public Class<CreditRatingProtocol.Poll> commandType() { return CreditRatingProtocol.Poll.class; }

    @Override
    public CreditRatingProtocol.Progress execute(
            CreditRatingProtocol.Poll command,
            ProviderInvocationContext context
    ) {
        Preconditions.requireNonNull(command, "command");
        String trackingCode = Preconditions.requireText(command.trackingCode(), "trackingCode");
        return new CreditRatingProtocol.Progress(
                CreditRatingProgressStatus.COMPLETED,
                trackingCode,
                0,
                "A2",
                8,
                BigDecimal.valueOf(720),
                null,
                null
        );
    }
}
