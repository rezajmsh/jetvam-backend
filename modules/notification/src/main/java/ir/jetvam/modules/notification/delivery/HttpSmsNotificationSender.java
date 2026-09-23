package ir.jetvam.modules.notification.delivery;

import ir.jetvam.common.text.TextUtils;
import ir.jetvam.infra.http.JetvamHttpClientFactory;
import ir.jetvam.modules.notification.config.NotificationProperties;
import ir.jetvam.modules.notification.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Sends template-based SMS messages through the configured external gateway.
 * Provider credentials and sensitive request values are excluded from logging.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Component
@RequiredArgsConstructor
public class HttpSmsNotificationSender implements NotificationChannelSender {

    private static final String CLIENT_NAME = "notification-sms";

    private final JetvamHttpClientFactory httpClientFactory;
    private final NotificationProperties properties;
    private volatile RestClient restClient;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public NotificationDeliveryResult send(NotificationDelivery delivery) {
        NotificationProperties.Sms settings = properties.getSms();
        try {
            ensureConfigured(settings);
            RestClient.RequestBodySpec request = client(settings)
                    .post()
                    .uri(settings.getPath())
                    .header("Idempotency-Key", delivery.notificationId().toString());
            if (TextUtils.hasText(settings.getApiKey())) {
                request.header(settings.getApiKeyHeader(), settings.getApiKey());
            }
            ResponseEntity<Void> response = request
                    .body(new SmsRequest(delivery.destination(), delivery.templateCode(), delivery.parameters()))
                    .retrieve()
                    .toBodilessEntity();
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw NotificationDeliveryException.permanent(
                        "HTTP_" + response.getStatusCode().value(),
                        new IllegalStateException("SMS provider returned a non-successful redirect status")
                );
            }
            return new NotificationDeliveryResult(response.getHeaders().getFirst("X-Message-Id"));
        } catch (NotificationDeliveryException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            boolean retryable = status == 408 || status == 429 || status >= 500;
            throw new NotificationDeliveryException("HTTP_" + status, retryable, exception);
        } catch (RuntimeException exception) {
            throw NotificationDeliveryException.retryable("TRANSPORT_ERROR", exception);
        }
    }

    private RestClient client(NotificationProperties.Sms settings) {
        RestClient current = restClient;
        if (current == null) {
            synchronized (this) {
                current = restClient;
                if (current == null) {
                    current = httpClientFactory.create(CLIENT_NAME, settings.getBaseUrl());
                    restClient = current;
                }
            }
        }
        return current;
    }

    private static void ensureConfigured(NotificationProperties.Sms settings) {
        if (!settings.isEnabled()
                || !TextUtils.hasText(settings.getBaseUrl())
                || !TextUtils.hasText(settings.getPath())) {
            throw NotificationDeliveryException.permanent(
                    "PROVIDER_NOT_CONFIGURED",
                    new IllegalStateException("SMS provider endpoint is not configured")
            );
        }
    }

    /**
     * Defines the generic template-based payload sent to an SMS gateway.
     * A provider-specific adapter can replace this sender without changing producers.
     *
     * @author reza jamshidi
     * @since 9/22/2026
     */
    private record SmsRequest(String destination, String templateCode, Map<String, String> parameters) {
    }
}
