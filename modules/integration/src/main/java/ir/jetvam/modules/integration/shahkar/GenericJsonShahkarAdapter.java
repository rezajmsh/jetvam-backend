package ir.jetvam.modules.integration.shahkar;

import ir.jetvam.modules.integration.IntegrationCapabilities;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import ir.jetvam.modules.integration.routing.ExternalProviderAdapter;
import ir.jetvam.modules.integration.routing.ProviderInvocationContext;
import ir.jetvam.modules.integration.routing.ProviderInvocationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * Implements the baseline JSON Shahkar contract as a reusable provider adapter.
 * Providers with different schemas or cryptographic signatures register another adapter code.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Component
@RequiredArgsConstructor
public class GenericJsonShahkarAdapter implements ExternalProviderAdapter<ShahkarCommand, ShahkarVerification> {

    public static final String ADAPTER_CODE = "SHAHKAR_HTTP_JSON_V1";

    private final DynamicProviderHttpClientFactory clientFactory;

    @Override
    public String capabilityCode() {
        return IntegrationCapabilities.SHAHKAR_VERIFY;
    }

    @Override
    public String adapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public Class<ShahkarCommand> commandType() {
        return ShahkarCommand.class;
    }

    @Override
    public ShahkarVerification execute(ShahkarCommand command, ProviderInvocationContext context) {
        try {
            ShahkarResponse response = clientFactory.get(context)
                    .post()
                    .uri(context.provider().operationPath())
                    .body(new ShahkarRequest(command.mobile(), command.nationalCode()))
                    .retrieve()
                    .body(ShahkarResponse.class);
            if (response == null) {
                throw ProviderInvocationException.retryable(
                        "EMPTY_RESPONSE", false, new IllegalStateException("Shahkar provider returned no body")
                );
            }
            return new ShahkarVerification(response.matched(), response.trackingId());
        } catch (ProviderInvocationException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            boolean retryable = status == 408 || status == 429 || status >= 500;
            if (retryable) {
                throw ProviderInvocationException.retryable("HTTP_" + status, false, exception);
            }
            throw ProviderInvocationException.permanent("HTTP_" + status, exception);
        } catch (RuntimeException exception) {
            throw ProviderInvocationException.retryable("TRANSPORT_ERROR", false, exception);
        }
    }

    private record ShahkarRequest(String mobile, String nationalCode) {
    }

    private record ShahkarResponse(boolean matched, String trackingId) {
    }
}
