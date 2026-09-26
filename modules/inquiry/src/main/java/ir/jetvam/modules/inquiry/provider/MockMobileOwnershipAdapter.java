package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.integration.routing.ExternalProviderAdapter;
import ir.jetvam.modules.integration.routing.ProviderInvocationContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Supplies a configurable in-process Shahkar result for local development.
 * It still participates in provider routing so switching to a real provider does not change registration logic.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
@Component
public final class MockMobileOwnershipAdapter implements ExternalProviderAdapter<
        InquiryRequests.MobileOwnership,
        InquiryResults.MobileOwnership> {

    public static final String ADAPTER_CODE = "SHAHKAR_MOCK_V1";

    @Override
    public String capabilityCode() {
        return InquiryCapabilities.MOBILE_OWNERSHIP;
    }

    @Override
    public String adapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public Class<InquiryRequests.MobileOwnership> commandType() {
        return InquiryRequests.MobileOwnership.class;
    }

    @Override
    public InquiryResults.MobileOwnership execute(
            InquiryRequests.MobileOwnership command,
        ProviderInvocationContext context
    ) {
        Preconditions.requireNonNull(command, "command");
        return new InquiryResults.MobileOwnership(true, "MOCK-SHAHKAR-" + UUID.randomUUID());
    }
}
