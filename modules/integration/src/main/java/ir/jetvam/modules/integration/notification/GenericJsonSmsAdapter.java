package ir.jetvam.modules.integration.notification;

import ir.jetvam.modules.integration.IntegrationCapabilities;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import ir.jetvam.modules.integration.routing.ExternalProviderAdapter;
import ir.jetvam.modules.integration.routing.ProviderInvocationContext;
import ir.jetvam.modules.integration.routing.ProviderInvocationException;
import ir.jetvam.modules.notification.delivery.NotificationDelivery;
import ir.jetvam.modules.notification.delivery.NotificationDeliveryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.util.Map;

/**
 * Implements the baseline template-based JSON SMS contract outside notification business logic.
 * A vendor with another schema or signature supplies a separate adapter implementation.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Component
@RequiredArgsConstructor
public class GenericJsonSmsAdapter implements ExternalProviderAdapter<NotificationDelivery, NotificationDeliveryResult> {

    public static final String ADAPTER_CODE = "SMS_HTTP_JSON_V1";

    private final DynamicProviderHttpClientFactory clientFactory;

    @Override
    public String capabilityCode() {
        return IntegrationCapabilities.SMS_SEND;
    }

    @Override
    public String adapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public Class<NotificationDelivery> commandType() {
        return NotificationDelivery.class;
    }

    @Override
    public NotificationDeliveryResult execute(NotificationDelivery command, ProviderInvocationContext context) {
        try {
            ResponseEntity<Void> response = clientFactory.get(context)
                    .post()
                    .uri(context.provider().operationPath())
                    .header("Idempotency-Key", command.notificationId().toString())
                    .body(new SmsRequest(command.destination(), command.templateCode(), command.parameters()))
                    .retrieve()
                    .toBodilessEntity();
            return new NotificationDeliveryResult(response.getHeaders().getFirst("X-Message-Id"));
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            boolean retryable = status == 408 || status == 429 || status >= 500;
            if (retryable) {
                throw ProviderInvocationException.retryable("HTTP_" + status, status >= 500, exception);
            }
            throw ProviderInvocationException.permanent("HTTP_" + status, exception);
        } catch (ResourceAccessException exception) {
            boolean reachedProvider = !hasCause(exception, ConnectException.class);
            throw ProviderInvocationException.retryable("TRANSPORT_ERROR", reachedProvider, exception);
        } catch (RuntimeException exception) {
            throw ProviderInvocationException.retryable("TRANSPORT_ERROR", true, exception);
        }
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record SmsRequest(String destination, String templateCode, Map<String, String> parameters) {
    }
}
