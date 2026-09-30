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
 * Provides a valid and alive civil-registration result for local environments.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Component
public final class MockCivilRegistrationAdapter implements ExternalProviderAdapter<InquiryRequests.CivilRegistration, InquiryResults.CivilRegistration> {

    public static final String ADAPTER_CODE = "CIVIL_REGISTRATION_MOCK_V1";

    @Override public String capabilityCode() { return InquiryType.CIVIL_REGISTRATION.code(); }
    @Override public String adapterCode() { return ADAPTER_CODE; }
    @Override public Class<InquiryRequests.CivilRegistration> commandType() { return InquiryRequests.CivilRegistration.class; }

    @Override
    public InquiryResults.CivilRegistration execute(InquiryRequests.CivilRegistration command, ProviderInvocationContext context) {
        Preconditions.requireNonNull(command, "command");
        return new InquiryResults.CivilRegistration(true, true, "MOCK-CIVIL-" + UUID.randomUUID());
    }
}
