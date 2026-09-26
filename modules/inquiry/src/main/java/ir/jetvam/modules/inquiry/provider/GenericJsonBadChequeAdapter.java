package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Maps a baseline JSON bad-cheque provider to Jetvam's normalized inquiry outcome.
 * Provider-specific signatures remain isolated in adapter implementations inside Inquiry.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class GenericJsonBadChequeAdapter extends AbstractJsonInquiryAdapter<
        InquiryRequests.BadCheque,
        InquiryResults.BadCheque,
        GenericJsonBadChequeAdapter.Response> {

    public static final String ADAPTER_CODE = "BAD_CHEQUE_HTTP_JSON_V1";

    public GenericJsonBadChequeAdapter(DynamicProviderHttpClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public String capabilityCode() {
        return InquiryCapabilities.BAD_CHEQUE;
    }

    @Override
    public String adapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public Class<InquiryRequests.BadCheque> commandType() {
        return InquiryRequests.BadCheque.class;
    }

    @Override
    protected Object requestBody(InquiryRequests.BadCheque command) {
        return new Request(command.nationalCode());
    }

    @Override
    protected Class<Response> responseType() {
        return Response.class;
    }

    @Override
    protected InquiryResults.BadCheque toResult(Response response) {
        Preconditions.require(response.unsettledCount() >= 0, "Provider returned a negative unsettledCount");
        BigDecimal totalAmount = response.totalAmount() == null ? BigDecimal.ZERO : response.totalAmount();
        Preconditions.require(totalAmount.signum() >= 0, "Provider returned a negative totalAmount");
        return new InquiryResults.BadCheque(response.unsettledCount(), totalAmount, response.trackingId());
    }

    private record Request(String nationalCode) {
    }

    record Response(int unsettledCount, BigDecimal totalAmount, String trackingId) {
    }
}
