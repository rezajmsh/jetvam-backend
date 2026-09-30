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
 * Provides a debt-free banking-facilities result for local environments.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Component
public final class MockBankingFacilitiesAdapter implements ExternalProviderAdapter<InquiryRequests.BankingFacilities, InquiryResults.BankingFacilities> {

    public static final String ADAPTER_CODE = "BANKING_FACILITIES_MOCK_V1";

    @Override public String capabilityCode() { return InquiryType.BANKING_FACILITIES.code(); }
    @Override public String adapterCode() { return ADAPTER_CODE; }
    @Override public Class<InquiryRequests.BankingFacilities> commandType() { return InquiryRequests.BankingFacilities.class; }

    @Override
    public InquiryResults.BankingFacilities execute(InquiryRequests.BankingFacilities command, ProviderInvocationContext context) {
        Preconditions.requireNonNull(command, "command");
        return new InquiryResults.BankingFacilities(
                0, 0, false, BigDecimal.ZERO, "MOCK-FACILITIES-" + UUID.randomUUID()
        );
    }
}
