package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import ir.jetvam.modules.integration.routing.ExternalProviderAdapter;
import ir.jetvam.modules.integration.routing.ProviderInvocationContext;
import ir.jetvam.modules.integration.routing.ProviderInvocationException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Centralizes transport and failure mapping shared by JSON inquiry adapters.
 * Concrete adapters own only provider request/response translation for one capability.
 *
 * @param <C> canonical inquiry command
 * @param <R> canonical inquiry result
 * @param <W> provider wire response
 * @author reza jamshidi
 * @since 9/25/2026
 */
abstract class AbstractJsonInquiryAdapter<C, R, W> implements ExternalProviderAdapter<C, R> {

    private final DynamicProviderHttpClientFactory clientFactory;

    protected AbstractJsonInquiryAdapter(DynamicProviderHttpClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public final R execute(C command, ProviderInvocationContext context) {
        try {
            W response = clientFactory.get(context)
                    .post()
                    .uri(context.provider().operationPath())
                    .body(requestBody(command))
                    .retrieve()
                    .body(responseType());
            if (response == null) {
                throw ProviderInvocationException.retryable(
                        "EMPTY_RESPONSE", false, new IllegalStateException("Inquiry provider returned no body")
                );
            }
            return toResult(response);
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

    protected abstract Object requestBody(C command);

    protected abstract Class<W> responseType();

    protected abstract R toResult(W response);
}
