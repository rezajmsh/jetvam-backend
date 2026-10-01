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
 * Executes the banking-facilities inquiry and normalizes its provider result.
 *
 * @author reza jamshidi
 * @since 10/1/2026
 */
@Component
@RequiredArgsConstructor
public class BankingFacilitiesInquiryProvider implements TypedInquiryProvider<InquiryRequests.BankingFacilities> {

    private final RoutingInquiryService routingService;

    @Override
    public InquiryType inquiryType() {
        return InquiryType.BANKING_FACILITIES;
    }

    @Override
    public Class<InquiryRequests.BankingFacilities> requestType() {
        return InquiryRequests.BankingFacilities.class;
    }

    @Override
    public InquiryExecutionResult execute(
            String nationalCode,
            InquiryRequests.BankingFacilities request,
            InquiryExecutionContext context
    ) {
        ProviderExecution<InquiryResults.BankingFacilities> execution = routingService.findBankingFacilities(
                new InquiryRequests.BankingFacilities(nationalCode)
        );
        InquiryResults.BankingFacilities result = execution.result();
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("directFacilityCount", Integer.toString(result.directFacilityCount()));
        facts.put("indirectFacilityCount", Integer.toString(result.indirectFacilityCount()));
        facts.put("hasOverdueDebt", Boolean.toString(result.hasOverdueDebt()));
        facts.put("overdueAmount", result.overdueAmount().toPlainString());
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
