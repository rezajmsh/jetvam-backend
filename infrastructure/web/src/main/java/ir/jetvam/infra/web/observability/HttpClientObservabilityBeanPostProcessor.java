package ir.jetvam.infra.web.observability;

import io.micrometer.observation.ObservationRegistry;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class HttpClientObservabilityBeanPostProcessor implements BeanPostProcessor {

    private final ObjectProvider<HttpClientObservabilityInterceptor> interceptorProvider;
    private final ObjectProvider<ObservationRegistry> observationRegistryProvider;
    private final ObjectProvider<JetvamObservabilityProperties> propertiesProvider;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof RestTemplate) && !(bean instanceof RestClient.Builder)) {
            return bean;
        }
        JetvamObservabilityProperties properties = propertiesProvider.getIfAvailable();
        HttpClientObservabilityInterceptor interceptor = interceptorProvider.getIfAvailable();
        if (properties == null || interceptor == null || !properties.getHttp().getClient().isEnabled()) {
            return bean;
        }
        ObservationRegistry observationRegistry = observationRegistryProvider.getIfAvailable(
                () -> ObservationRegistry.NOOP
        );
        if (bean instanceof RestTemplate restTemplate) {
            restTemplate.setObservationRegistry(observationRegistry);
            if (restTemplate.getInterceptors().stream().noneMatch(HttpClientObservabilityInterceptor.class::isInstance)) {
                restTemplate.getInterceptors().add(interceptor);
            }
        } else if (bean instanceof RestClient.Builder builder) {
            builder.observationRegistry(observationRegistry);
            builder.requestInterceptors(interceptors -> {
                if (interceptors.stream().noneMatch(HttpClientObservabilityInterceptor.class::isInstance)) {
                    interceptors.add(interceptor);
                }
            });
        }
        return bean;
    }
}
