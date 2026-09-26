package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.DeferredInquiryStatus;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Submits a credit-rating request to a baseline two-step JSON provider.
 * Provider-specific signatures can be introduced as additional adapters without changing the workflow.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class GenericJsonCreditRatingSubmitAdapter extends AbstractJsonInquiryAdapter<
        CreditRatingProtocol.Submit,
        CreditRatingProtocol.Progress,
        GenericJsonCreditRatingSubmitAdapter.Response> {

    public static final String ADAPTER_CODE = "CREDIT_RATING_SUBMIT_HTTP_JSON_V1";

    public GenericJsonCreditRatingSubmitAdapter(DynamicProviderHttpClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public String capabilityCode() {
        return InquiryCapabilities.CREDIT_RATING_SUBMIT;
    }

    @Override
    public String adapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public Class<CreditRatingProtocol.Submit> commandType() {
        return CreditRatingProtocol.Submit.class;
    }

    @Override
    protected Object requestBody(CreditRatingProtocol.Submit command) {
        return new Request(command.nationalCode());
    }

    @Override
    protected Class<Response> responseType() {
        return Response.class;
    }

    @Override
    protected CreditRatingProtocol.Progress toResult(Response response) {
        return response.toProgress();
    }

    private record Request(String nationalCode) {
    }

    record Response(
            DeferredInquiryStatus status,
            String trackingCode,
            int retryAfterSeconds,
            String ratingCode,
            Integer rank,
            BigDecimal score,
            String rejectionCode,
            String rejectionMessage
    ) {
        CreditRatingProtocol.Progress toProgress() {
            return new CreditRatingProtocol.Progress(
                    status, trackingCode, retryAfterSeconds, ratingCode, rank, score,
                    rejectionCode, rejectionMessage
            );
        }
    }
}
