package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.DeferredInquiryStatus;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Polls a baseline two-step JSON credit-rating provider by its previously issued tracking code.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class GenericJsonCreditRatingPollAdapter extends AbstractJsonInquiryAdapter<
        CreditRatingProtocol.Poll,
        CreditRatingProtocol.Progress,
        GenericJsonCreditRatingPollAdapter.Response> {

    public static final String ADAPTER_CODE = "CREDIT_RATING_POLL_HTTP_JSON_V1";

    public GenericJsonCreditRatingPollAdapter(DynamicProviderHttpClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public String capabilityCode() {
        return InquiryCapabilities.CREDIT_RATING_POLL;
    }

    @Override
    public String adapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public Class<CreditRatingProtocol.Poll> commandType() {
        return CreditRatingProtocol.Poll.class;
    }

    @Override
    protected Object requestBody(CreditRatingProtocol.Poll command) {
        return new Request(command.trackingCode());
    }

    @Override
    protected Class<Response> responseType() {
        return Response.class;
    }

    @Override
    protected CreditRatingProtocol.Progress toResult(Response response) {
        return response.toProgress();
    }

    private record Request(String trackingCode) {
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
