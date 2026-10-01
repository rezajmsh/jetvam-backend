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
 * Executes the bad-cheque inquiry and normalizes its provider result.
 *
 * @author reza jamshidi
 * @since 10/1/2026
 */
@Component
@RequiredArgsConstructor
public class BadChequeInquiryProvider implements TypedInquiryProvider<InquiryRequests.BadCheque> {

    private final RoutingInquiryService routingService;

    @Override
    public InquiryType inquiryType() {
        return InquiryType.BAD_CHEQUE;
    }

    @Override
    public Class<InquiryRequests.BadCheque> requestType() {
        return InquiryRequests.BadCheque.class;
    }

    @Override
    public InquiryExecutionResult execute(
            String nationalCode,
            InquiryRequests.BadCheque request,
            InquiryExecutionContext context
    ) {
        ProviderExecution<InquiryResults.BadCheque> execution = routingService.findBadCheques(
                new InquiryRequests.BadCheque(nationalCode)
        );
        InquiryResults.BadCheque result = execution.result();
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("unsettledCount", Integer.toString(result.unsettledCount()));
        facts.put("totalAmount", result.totalAmount().toPlainString());
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
