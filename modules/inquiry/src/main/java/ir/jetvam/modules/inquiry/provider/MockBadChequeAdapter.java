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
 * Provides a successful bad-cheque response for local and acceptance environments.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Component
public final class MockBadChequeAdapter implements ExternalProviderAdapter<InquiryRequests.BadCheque, InquiryResults.BadCheque> {

    public static final String ADAPTER_CODE = "BAD_CHEQUE_MOCK_V1";

    @Override public String capabilityCode() { return InquiryType.BAD_CHEQUE.code(); }
    @Override public String adapterCode() { return ADAPTER_CODE; }
    @Override public Class<InquiryRequests.BadCheque> commandType() { return InquiryRequests.BadCheque.class; }

    @Override
    public InquiryResults.BadCheque execute(InquiryRequests.BadCheque command, ProviderInvocationContext context) {
        Preconditions.requireNonNull(command, "command");
        return new InquiryResults.BadCheque(0, BigDecimal.ZERO, tracking("CHEQUE"));
    }

    private static String tracking(String prefix) { return "MOCK-" + prefix + "-" + UUID.randomUUID(); }
}
