package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import org.springframework.stereotype.Component;

/**
 * Maps a baseline bank-account JSON response to normalized account-status facts.
 * Bank-specific status values remain data while active state is exposed consistently.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class GenericJsonBankAccountStatusAdapter extends AbstractJsonInquiryAdapter<
        InquiryRequests.BankAccountStatus,
        InquiryResults.BankAccountStatus,
        GenericJsonBankAccountStatusAdapter.Response> {

    public static final String ADAPTER_CODE = "BANK_ACCOUNT_STATUS_HTTP_JSON_V1";

    public GenericJsonBankAccountStatusAdapter(DynamicProviderHttpClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override public String capabilityCode() { return InquiryCapabilities.BANK_ACCOUNT_STATUS; }

    @Override public String adapterCode() { return ADAPTER_CODE; }

    @Override public Class<InquiryRequests.BankAccountStatus> commandType() {
        return InquiryRequests.BankAccountStatus.class;
    }

    @Override protected Object requestBody(InquiryRequests.BankAccountStatus command) {
        return new Request(command.nationalCode());
    }

    @Override protected Class<Response> responseType() { return Response.class; }

    @Override protected InquiryResults.BankAccountStatus toResult(Response response) {
        String code = Preconditions.requireText(response.statusCode(), "provider.statusCode").strip().toUpperCase();
        return new InquiryResults.BankAccountStatus(code, response.active(), response.trackingId());
    }

    private record Request(String nationalCode) {
    }

    record Response(String statusCode, boolean active, String trackingId) {
    }
}
