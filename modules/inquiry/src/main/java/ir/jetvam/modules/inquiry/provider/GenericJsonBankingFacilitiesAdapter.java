package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Maps direct and indirect banking-facility JSON data to normalized applicant facts.
 * Overdue exposure is retained separately from facility counts for later policy controls.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class GenericJsonBankingFacilitiesAdapter extends AbstractJsonInquiryAdapter<
        InquiryRequests.BankingFacilities,
        InquiryResults.BankingFacilities,
        GenericJsonBankingFacilitiesAdapter.Response> {

    public static final String ADAPTER_CODE = "BANKING_FACILITIES_HTTP_JSON_V1";

    public GenericJsonBankingFacilitiesAdapter(DynamicProviderHttpClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override public String capabilityCode() { return InquiryCapabilities.BANKING_FACILITIES; }

    @Override public String adapterCode() { return ADAPTER_CODE; }

    @Override public Class<InquiryRequests.BankingFacilities> commandType() {
        return InquiryRequests.BankingFacilities.class;
    }

    @Override protected Object requestBody(InquiryRequests.BankingFacilities command) {
        return new Request(command.nationalCode());
    }

    @Override protected Class<Response> responseType() { return Response.class; }

    @Override protected InquiryResults.BankingFacilities toResult(Response response) {
        Preconditions.require(response.directFacilityCount() >= 0,
                "Provider returned a negative directFacilityCount");
        Preconditions.require(response.indirectFacilityCount() >= 0,
                "Provider returned a negative indirectFacilityCount");
        BigDecimal overdueAmount = response.overdueAmount() == null ? BigDecimal.ZERO : response.overdueAmount();
        Preconditions.require(overdueAmount.signum() >= 0, "Provider returned a negative overdueAmount");
        return new InquiryResults.BankingFacilities(
                response.directFacilityCount(), response.indirectFacilityCount(), response.hasOverdueDebt(),
                overdueAmount, response.trackingId()
        );
    }

    private record Request(String nationalCode) {
    }

    record Response(
            int directFacilityCount,
            int indirectFacilityCount,
            boolean hasOverdueDebt,
            BigDecimal overdueAmount,
            String trackingId
    ) {
    }
}
