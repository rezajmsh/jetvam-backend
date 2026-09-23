package ir.jetvam.apps.uaa.integration;

import ir.jetvam.apps.uaa.config.JetvamUaaProperties;
import ir.jetvam.common.exception.IntegrationException;
import ir.jetvam.common.text.TextUtils;
import ir.jetvam.infra.http.JetvamHttpClientFactory;
import ir.jetvam.modules.identity.service.ShahkarProvider;
import ir.jetvam.modules.identity.service.ShahkarVerification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapts the configured Shahkar HTTP contract to the identity provider port.
 * Transport failures are normalized through the common integration exception hierarchy.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Component
@RequiredArgsConstructor
public class HttpShahkarProvider implements ShahkarProvider {

    private final JetvamHttpClientFactory httpClientFactory;
    private final JetvamUaaProperties properties;
    private volatile RestClient restClient;

    @Override
    public ShahkarVerification verify(String mobile, String nationalCode) {
        JetvamUaaProperties.Endpoint endpoint = properties.getProviders().getShahkar();
        ensureEnabled(endpoint);
        try {
            RestClient.RequestBodySpec request = client(endpoint)
                    .post()
                    .uri(endpoint.getPath());
            if (TextUtils.hasText(endpoint.getApiKey())) {
                request.header(endpoint.getApiKeyHeader(), endpoint.getApiKey());
            }
            ShahkarResponse response = request.body(new ShahkarRequest(mobile, nationalCode))
                    .retrieve()
                    .body(ShahkarResponse.class);
            if (response == null) {
                throw new IllegalStateException("Shahkar provider returned an empty response");
            }
            return new ShahkarVerification(response.matched(), response.trackingId());
        } catch (RuntimeException exception) {
            throw new IntegrationException("shahkar", "verify", exception);
        }
    }

    private RestClient client(JetvamUaaProperties.Endpoint endpoint) {
        RestClient current = restClient;
        if (current == null) {
            synchronized (this) {
                current = restClient;
                if (current == null) {
                    current = httpClientFactory.create("shahkar", endpoint.getBaseUrl());
                    restClient = current;
                }
            }
        }
        return current;
    }

    private static void ensureEnabled(JetvamUaaProperties.Endpoint endpoint) {
        if (!endpoint.isEnabled() || !TextUtils.hasText(endpoint.getBaseUrl()) || !TextUtils.hasText(endpoint.getPath())) {
            throw new IntegrationException("shahkar", "configuration",
                    new IllegalStateException("Provider endpoint is not configured"));
        }
    }

    /**
     * Defines the outbound Shahkar lookup payload at the HTTP boundary.
     * Domain services remain independent of the provider's wire format.
     *
     * @author reza jamshidi
     * @since 9/22/2026
     */
    private record ShahkarRequest(String mobile, String nationalCode) {
    }

    /**
     * Maps the Shahkar ownership decision and provider tracking identifier.
     * Unknown fields from a concrete provider can be ignored safely.
     *
     * @author reza jamshidi
     * @since 9/22/2026
     */
    private record ShahkarResponse(boolean matched, String trackingId) {
    }
}
