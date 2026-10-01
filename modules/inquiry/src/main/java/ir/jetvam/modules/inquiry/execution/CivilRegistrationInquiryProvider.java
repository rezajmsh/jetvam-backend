package ir.jetvam.modules.inquiry.execution;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.inquiry.service.RoutingInquiryService;
import ir.jetvam.modules.integration.routing.ProviderExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes the civil-registration inquiry and normalizes its provider result.
 *
 * @author reza jamshidi
 * @since 10/1/2026
 */
@Component
@RequiredArgsConstructor
public class CivilRegistrationInquiryProvider implements TypedInquiryProvider<InquiryRequests.CivilRegistration> {

    private final RoutingInquiryService routingService;

    @Override
    public InquiryType inquiryType() {
        return InquiryType.CIVIL_REGISTRATION;
    }

    @Override
    public Class<InquiryRequests.CivilRegistration> requestType() {
        return InquiryRequests.CivilRegistration.class;
    }

    @Override
    public InquiryExecutionResult execute(
            String nationalCode,
            InquiryRequests.CivilRegistration request,
            InquiryExecutionContext context
    ) {
        ProviderExecution<InquiryResults.CivilRegistration> execution = routingService.findCivilRegistration(
                new InquiryRequests.CivilRegistration(nationalCode)
        );
        InquiryResults.CivilRegistration result = execution.result();
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("identityValid", Boolean.toString(result.identityValid()));
        facts.put("alive", Boolean.toString(result.alive()));
        put(facts, "trackingId", result.trackingId());
        return InquiryExecutionResult.completed(
                new InquiryExecutionContext(execution.providerCode(), Map.of()), result.trackingId(), facts
        );
    }

    private static void put(Map<String, String> facts, String key, String value) {
        if (value != null && !value.isBlank()) {
            facts.put(key, value);
        }
    }
}
