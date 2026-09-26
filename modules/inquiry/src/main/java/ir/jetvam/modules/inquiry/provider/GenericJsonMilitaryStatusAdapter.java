package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import org.springframework.stereotype.Component;

/**
 * Maps a baseline military-status JSON response to normalized eligibility facts.
 * The provider status code remains available without leaking its wire response model.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class GenericJsonMilitaryStatusAdapter extends AbstractJsonInquiryAdapter<
        InquiryRequests.MilitaryStatus,
        InquiryResults.MilitaryStatus,
        GenericJsonMilitaryStatusAdapter.Response> {

    public static final String ADAPTER_CODE = "MILITARY_STATUS_HTTP_JSON_V1";

    public GenericJsonMilitaryStatusAdapter(DynamicProviderHttpClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override public String capabilityCode() { return InquiryCapabilities.MILITARY_STATUS; }

    @Override public String adapterCode() { return ADAPTER_CODE; }

    @Override public Class<InquiryRequests.MilitaryStatus> commandType() {
        return InquiryRequests.MilitaryStatus.class;
    }

    @Override protected Object requestBody(InquiryRequests.MilitaryStatus command) {
        return new Request(command.nationalCode());
    }

    @Override protected Class<Response> responseType() { return Response.class; }

    @Override protected InquiryResults.MilitaryStatus toResult(Response response) {
        String code = Preconditions.requireText(response.statusCode(), "provider.statusCode").strip().toUpperCase();
        return new InquiryResults.MilitaryStatus(code, response.eligible(), response.trackingId());
    }

    private record Request(String nationalCode) {
    }

    record Response(String statusCode, boolean eligible, String trackingId) {
    }
}
