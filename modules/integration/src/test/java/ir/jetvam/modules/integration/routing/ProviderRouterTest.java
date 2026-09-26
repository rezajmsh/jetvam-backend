package ir.jetvam.modules.integration.routing;

import ir.jetvam.modules.integration.model.ProviderAuthenticationType;
import ir.jetvam.modules.integration.model.ProviderRoutingMode;
import ir.jetvam.modules.integration.service.ExternalProviderView;
import ir.jetvam.modules.integration.service.ProviderConfigurationService;
import ir.jetvam.modules.integration.service.ProviderRouteView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies failover boundaries for retryable, permanent and ambiguous provider failures.
 * Routing decisions operate on canonical commands rather than provider wire contracts.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
class ProviderRouterTest {

    private ProviderConfigurationService configurationService;
    private ProviderAdapterRegistry adapterRegistry;
    private ProviderRouter router;

    @BeforeEach
    void setUp() {
        configurationService = mock(ProviderConfigurationService.class);
        adapterRegistry = mock(ProviderAdapterRegistry.class);
        router = new ProviderRouter(configurationService, adapterRegistry, new ProviderCircuitRegistry());
        when(configurationService.getRoute("TEST")).thenReturn(new ProviderRouteView(
                "TEST", ProviderRoutingMode.PRIORITY_FAILOVER, true, null, null, 3, 30, 0
        ));
        when(configurationService.findEnabledProviders("TEST")).thenReturn(List.of(
                provider("FIRST", "ADAPTER_A", 1), provider("SECOND", "ADAPTER_B", 2)
        ));
        when(configurationService.findTlsProfile(null)).thenReturn(Optional.empty());
    }

    @Test
    void failsOverAfterRetryableAvailabilityFailure() {
        AtomicInteger secondCalls = new AtomicInteger();
        doReturn(new TestAdapter(
                "ADAPTER_A", ignored -> {
                    throw ProviderInvocationException.retryable("TIMEOUT", false, new IllegalStateException());
                }
        )).when(adapterRegistry).get("TEST", "ADAPTER_A");
        doReturn(new TestAdapter(
                "ADAPTER_B", value -> {
                    secondCalls.incrementAndGet();
                    return value + "-ok";
                }
        )).when(adapterRegistry).get("TEST", "ADAPTER_B");

        assertThat(router.execute("TEST", "request", String.class, false)).isEqualTo("request-ok");
        assertThat(secondCalls).hasValue(1);
    }

    @Test
    void doesNotFailOverAfterPermanentBusinessFailure() {
        AtomicInteger secondCalls = new AtomicInteger();
        doReturn(new TestAdapter(
                "ADAPTER_A", ignored -> {
                    throw ProviderInvocationException.permanent("INVALID_REQUEST", new IllegalStateException());
                }
        )).when(adapterRegistry).get("TEST", "ADAPTER_A");
        doReturn(new TestAdapter(
                "ADAPTER_B", value -> {
                    secondCalls.incrementAndGet();
                    return value;
                }
        )).when(adapterRegistry).get("TEST", "ADAPTER_B");

        assertThatThrownBy(() -> router.execute("TEST", "request", String.class, true))
                .isInstanceOf(ProviderInvocationException.class)
                .hasMessageContaining("INVALID_REQUEST");
        assertThat(secondCalls).hasValue(0);
    }

    @Test
    void doesNotFailOverAmbiguousUnsafeWrite() {
        doReturn(new TestAdapter(
                "ADAPTER_A", ignored -> {
                    throw ProviderInvocationException.retryable("READ_TIMEOUT", true, new IllegalStateException());
                }
        )).when(adapterRegistry).get("TEST", "ADAPTER_A");

        assertThatThrownBy(() -> router.execute("TEST", "request", String.class, false))
                .isInstanceOf(ProviderInvocationException.class)
                .hasMessageContaining("READ_TIMEOUT");
    }

    @Test
    void returnsSelectedProviderAndPinsAStatefulFollowUp() {
        doReturn(new TestAdapter("ADAPTER_A", value -> value + "-first"))
                .when(adapterRegistry).get("TEST", "ADAPTER_A");
        doReturn(new TestAdapter("ADAPTER_B", value -> value + "-second"))
                .when(adapterRegistry).get("TEST", "ADAPTER_B");

        ProviderExecution<String> submitted = router.executeWithProvider(
                "TEST", "request", String.class, true
        );
        String polled = router.executeOnProvider("TEST", "SECOND", "tracking", String.class);

        assertThat(submitted.providerCode()).isEqualTo("FIRST");
        assertThat(submitted.result()).isEqualTo("request-first");
        assertThat(polled).isEqualTo("tracking-second");
    }

    private static ExternalProviderView provider(String providerCode, String adapterCode, int priority) {
        return new ExternalProviderView(
                "TEST", providerCode, adapterCode, true, priority, 1, "https://provider.test", "/call",
                ProviderAuthenticationType.NONE, null, null, null, null, 1000, 2000, "{}", 0
        );
    }

    private record TestAdapter(
            String adapterCode,
            Function<String, String> function
    ) implements ExternalProviderAdapter<String, String> {

        @Override
        public String capabilityCode() {
            return "TEST";
        }

        @Override
        public Class<String> commandType() {
            return String.class;
        }

        @Override
        public String execute(String command, ProviderInvocationContext context) {
            return function.apply(command);
        }
    }
}
