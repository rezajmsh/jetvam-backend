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
 * Provides an eligible military-status result for local environments.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Component
public final class MockMilitaryStatusAdapter implements ExternalProviderAdapter<InquiryRequests.MilitaryStatus, InquiryResults.MilitaryStatus> {

    public static final String ADAPTER_CODE = "MILITARY_STATUS_MOCK_V1";

    @Override public String capabilityCode() { return InquiryType.MILITARY_STATUS.code(); }
    @Override public String adapterCode() { return ADAPTER_CODE; }
    @Override public Class<InquiryRequests.MilitaryStatus> commandType() { return InquiryRequests.MilitaryStatus.class; }

    @Override
    public InquiryResults.MilitaryStatus execute(InquiryRequests.MilitaryStatus command, ProviderInvocationContext context) {
        Preconditions.requireNonNull(command, "command");
        return new InquiryResults.MilitaryStatus("ELIGIBLE", true, "MOCK-MILITARY-" + UUID.randomUUID());
    }
}
