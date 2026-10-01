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
 * Executes the military-status inquiry and normalizes its provider result.
 *
 * @author reza jamshidi
 * @since 10/1/2026
 */
@Component
@RequiredArgsConstructor
public class MilitaryStatusInquiryProvider implements TypedInquiryProvider<InquiryRequests.MilitaryStatus> {

    private final RoutingInquiryService routingService;

    @Override
    public InquiryType inquiryType() {
        return InquiryType.MILITARY_STATUS;
    }

    @Override
    public Class<InquiryRequests.MilitaryStatus> requestType() {
        return InquiryRequests.MilitaryStatus.class;
    }

    @Override
    public InquiryExecutionResult execute(
            String nationalCode,
            InquiryRequests.MilitaryStatus request,
            InquiryExecutionContext context
    ) {
        ProviderExecution<InquiryResults.MilitaryStatus> execution = routingService.findMilitaryStatus(
                new InquiryRequests.MilitaryStatus(nationalCode)
        );
        InquiryResults.MilitaryStatus result = execution.result();
        Map<String, String> facts = new LinkedHashMap<>();
        put(facts, "statusCode", result.statusCode());
        facts.put("eligible", Boolean.toString(result.eligible()));
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
