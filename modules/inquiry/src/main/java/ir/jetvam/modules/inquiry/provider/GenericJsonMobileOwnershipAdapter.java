package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import org.springframework.stereotype.Component;

/**
 * Maps the generic Shahkar JSON protocol to the mobile-ownership inquiry contract.
 * A provider with another schema or signature registers a different adapter code.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class GenericJsonMobileOwnershipAdapter extends AbstractJsonInquiryAdapter<
        InquiryRequests.MobileOwnership,
        InquiryResults.MobileOwnership,
        GenericJsonMobileOwnershipAdapter.Response> {

    public static final String ADAPTER_CODE = "SHAHKAR_HTTP_JSON_V1";

    public GenericJsonMobileOwnershipAdapter(DynamicProviderHttpClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public String capabilityCode() {
        return InquiryCapabilities.MOBILE_OWNERSHIP;
    }

    @Override
    public String adapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public Class<InquiryRequests.MobileOwnership> commandType() {
        return InquiryRequests.MobileOwnership.class;
    }

    @Override
    protected Object requestBody(InquiryRequests.MobileOwnership command) {
        return new Request(command.mobile(), command.nationalCode());
    }

    @Override
    protected Class<Response> responseType() {
        return Response.class;
    }

    @Override
    protected InquiryResults.MobileOwnership toResult(Response response) {
        return new InquiryResults.MobileOwnership(response.matched(), response.trackingId());
    }

    private record Request(String mobile, String nationalCode) {
    }

    record Response(boolean matched, String trackingId) {
    }
}
