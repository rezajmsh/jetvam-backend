package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import org.springframework.stereotype.Component;

/**
 * Maps a baseline civil-registration JSON response to normalized identity facts.
 * Provider-specific schemas and signatures remain isolated behind the adapter code.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class GenericJsonCivilRegistrationAdapter extends AbstractJsonInquiryAdapter<
        InquiryRequests.CivilRegistration,
        InquiryResults.CivilRegistration,
        GenericJsonCivilRegistrationAdapter.Response> {

    public static final String ADAPTER_CODE = "CIVIL_REGISTRATION_HTTP_JSON_V1";

    public GenericJsonCivilRegistrationAdapter(DynamicProviderHttpClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override public String capabilityCode() { return InquiryCapabilities.CIVIL_REGISTRATION; }

    @Override public String adapterCode() { return ADAPTER_CODE; }

    @Override public Class<InquiryRequests.CivilRegistration> commandType() {
        return InquiryRequests.CivilRegistration.class;
    }

    @Override protected Object requestBody(InquiryRequests.CivilRegistration command) {
        return new Request(command.nationalCode());
    }

    @Override protected Class<Response> responseType() { return Response.class; }

    @Override protected InquiryResults.CivilRegistration toResult(Response response) {
        return new InquiryResults.CivilRegistration(
                response.identityValid(), response.alive(), response.trackingId()
        );
    }

    private record Request(String nationalCode) {
    }

    record Response(boolean identityValid, boolean alive, String trackingId) {
    }
}
