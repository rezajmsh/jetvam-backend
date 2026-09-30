package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.integration.routing.ExternalProviderAdapter;
import ir.jetvam.modules.integration.routing.ProviderInvocationContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Provides an active bank-account result for local environments.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Component
public final class MockBankAccountStatusAdapter implements ExternalProviderAdapter<InquiryRequests.BankAccountStatus, InquiryResults.BankAccountStatus> {

    public static final String ADAPTER_CODE = "BANK_ACCOUNT_STATUS_MOCK_V1";

    @Override public String capabilityCode() { return InquiryType.BANK_ACCOUNT_STATUS.code(); }
    @Override public String adapterCode() { return ADAPTER_CODE; }
    @Override public Class<InquiryRequests.BankAccountStatus> commandType() { return InquiryRequests.BankAccountStatus.class; }

    @Override
    public InquiryResults.BankAccountStatus execute(InquiryRequests.BankAccountStatus command, ProviderInvocationContext context) {
        Preconditions.requireNonNull(command, "command");
        return new InquiryResults.BankAccountStatus("ACTIVE", true, "MOCK-ACCOUNT-" + UUID.randomUUID());
    }
}
