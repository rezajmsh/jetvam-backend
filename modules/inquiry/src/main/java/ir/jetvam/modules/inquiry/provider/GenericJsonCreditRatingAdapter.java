package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Maps a baseline JSON credit bureau response to a provider-neutral rating and rank.
 * The adapter is responsible for normalizing each provider's scale to a higher-is-better rank.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class GenericJsonCreditRatingAdapter extends AbstractJsonInquiryAdapter<
        InquiryRequests.CreditRating,
        InquiryResults.CreditRating,
        GenericJsonCreditRatingAdapter.Response> {

    public static final String ADAPTER_CODE = "CREDIT_RATING_HTTP_JSON_V1";

    public GenericJsonCreditRatingAdapter(DynamicProviderHttpClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public String capabilityCode() {
        return InquiryCapabilities.CREDIT_RATING;
    }

    @Override
    public String adapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public Class<InquiryRequests.CreditRating> commandType() {
        return InquiryRequests.CreditRating.class;
    }

    @Override
    protected Object requestBody(InquiryRequests.CreditRating command) {
        return new Request(command.nationalCode());
    }

    @Override
    protected Class<Response> responseType() {
        return Response.class;
    }

    @Override
    protected InquiryResults.CreditRating toResult(Response response) {
        String ratingCode = Preconditions.requireText(response.ratingCode(), "provider.ratingCode").strip().toUpperCase();
        int rank = Preconditions.requireNonNegative(response.rank(), "provider.rank");
        if (response.score() != null) {
            Preconditions.require(response.score().signum() >= 0, "Provider returned a negative score");
        }
        return new InquiryResults.CreditRating(ratingCode, rank, response.score(), response.trackingId());
    }

    private record Request(String nationalCode) {
    }

    record Response(String ratingCode, int rank, BigDecimal score, String trackingId) {
    }
}
