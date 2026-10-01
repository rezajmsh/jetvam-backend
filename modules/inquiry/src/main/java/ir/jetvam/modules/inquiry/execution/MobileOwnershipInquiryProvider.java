package ir.jetvam.modules.inquiry.execution;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.inquiry.service.RoutingInquiryService;
import ir.jetvam.modules.integration.routing.ProviderExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes the mobile-ownership inquiry and normalizes its provider result.
 *
 * @author reza jamshidi
 * @since 10/1/2026
 */
@Component
@RequiredArgsConstructor
public class MobileOwnershipInquiryProvider implements TypedInquiryProvider<InquiryRequests.MobileOwnership> {

    private final RoutingInquiryService routingService;

    @Override
    public InquiryType inquiryType() {
        return InquiryType.MOBILE_OWNERSHIP;
    }

    @Override
    public Class<InquiryRequests.MobileOwnership> requestType() {
        return InquiryRequests.MobileOwnership.class;
    }

    @Override
    public InquiryExecutionResult execute(
            String nationalCode,
            InquiryRequests.MobileOwnership request,
            InquiryExecutionContext context
    ) {
        String mobile = IranianIdentifiers.normalizeMobileNumber(request.mobile());
        Preconditions.require(IranianIdentifiers.isValidMobileNumber(mobile), "mobile is invalid");
        ProviderExecution<InquiryResults.MobileOwnership> execution = routingService.verifyMobileOwnership(
                new InquiryRequests.MobileOwnership(mobile, nationalCode)
        );
        InquiryResults.MobileOwnership result = execution.result();
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("matched", Boolean.toString(result.matched()));
        put(facts, "trackingId", result.trackingId());
        return InquiryExecutionResult.completed(
                new InquiryExecutionContext(execution.providerCode(), Map.of()),
                result.trackingId(),
                facts
        );
    }

    private static void put(Map<String, String> facts, String key, String value) {
        if (value != null && !value.isBlank()) {
            facts.put(key, value);
        }
    }
}
