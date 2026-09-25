package ir.jetvam.modules.integration.routing;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.integration.model.ProviderRoutingMode;
import ir.jetvam.modules.integration.service.ExternalProviderView;
import ir.jetvam.modules.integration.service.ProviderConfigurationService;
import ir.jetvam.modules.integration.service.ProviderRouteView;
import ir.jetvam.modules.integration.service.TlsProfileView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Routes canonical commands across configured adapters with health-aware failover.
 * Permanent business errors and unsafe ambiguous writes never cascade to another provider.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Component
@RequiredArgsConstructor
public class ProviderRouter {

    private final ProviderConfigurationService configurationService;
    private final ProviderAdapterRegistry adapterRegistry;
    private final ProviderCircuitRegistry circuitRegistry;
    private final ConcurrentHashMap<String, AtomicLong> roundRobinSequences = new ConcurrentHashMap<>();

    public <C, R> R execute(
            String capabilityCode,
            C command,
            Class<R> resultType,
            boolean safeToFailoverAfterAmbiguousFailure
    ) {
        Preconditions.requireNonNull(command, "command");
        Preconditions.requireNonNull(resultType, "resultType");
        ExternalCallTransactionGuard.assertNoActiveTransaction(capabilityCode);
        ProviderRouteView route = configurationService.getRoute(capabilityCode);
        List<ExternalProviderView> candidates = orderedCandidates(
                route,
                configurationService.findEnabledProviders(route.capabilityCode()),
                Instant.now()
        );
        ProviderInvocationException lastFailure = null;
        for (ExternalProviderView provider : candidates) {
            if (!circuitRegistry.tryAcquire(route.capabilityCode(), provider.providerCode())) {
                continue;
            }
            try {
                R result = invoke(provider, command, resultType);
                circuitRegistry.recordSuccess(route.capabilityCode(), provider.providerCode());
                return result;
            } catch (ProviderInvocationException exception) {
                lastFailure = exception;
                if (!exception.isRetryable()) {
                    throw exception;
                }
                circuitRegistry.recordFailure(
                        route.capabilityCode(), provider.providerCode(), route.failureThreshold(),
                        route.openDurationSeconds()
                );
                if (!route.failoverEnabled()
                        || (exception.isAmbiguous() && !safeToFailoverAfterAmbiguousFailure)
                        || route.hasActiveOverride(Instant.now())) {
                    throw exception;
                }
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new ProviderUnavailableException(route.capabilityCode());
    }

    @SuppressWarnings("unchecked")
    private <C, R> R invoke(ExternalProviderView provider, C command, Class<R> resultType) {
        ExternalProviderAdapter<?, ?> candidate = adapterRegistry.get(
                provider.capabilityCode(), provider.adapterCode()
        );
        if (!candidate.commandType().isInstance(command)) {
            throw new IllegalStateException("Provider adapter command type does not match capability "
                    + provider.capabilityCode());
        }
        ExternalProviderAdapter<C, ?> adapter = (ExternalProviderAdapter<C, ?>) candidate;
        Optional<TlsProfileView> tlsProfile = configurationService.findTlsProfile(provider.tlsProfileCode());
        Object result = adapter.execute(command, new ProviderInvocationContext(provider, tlsProfile));
        if (!resultType.isInstance(result)) {
            throw new IllegalStateException("Provider adapter result type does not match capability "
                    + provider.capabilityCode());
        }
        return resultType.cast(result);
    }

    private List<ExternalProviderView> orderedCandidates(
            ProviderRouteView route,
            List<ExternalProviderView> providers,
            Instant now
    ) {
        if (route.hasActiveOverride(now)) {
            return providers.stream()
                    .filter(provider -> provider.providerCode().equals(route.forcedProviderCode()))
                    .findFirst().map(List::of)
                    .orElseThrow(() -> new ProviderUnavailableException(route.capabilityCode()));
        }
        if (route.routingMode() == ProviderRoutingMode.MANUAL_ONLY) {
            throw new ProviderUnavailableException(route.capabilityCode());
        }
        List<ExternalProviderView> ordered = new ArrayList<>(providers);
        ordered.sort(Comparator.comparingInt(ExternalProviderView::priority)
                .thenComparing(ExternalProviderView::providerCode));
        if (ordered.size() < 2) {
            return ordered;
        }
        return switch (route.routingMode()) {
            case PRIORITY_FAILOVER -> ordered;
            case ROUND_ROBIN -> rotate(route.capabilityCode(), ordered);
            case WEIGHTED -> weightedFirst(ordered);
            case MANUAL_ONLY -> List.of();
        };
    }

    private List<ExternalProviderView> rotate(String capabilityCode, List<ExternalProviderView> providers) {
        long sequence = roundRobinSequences.computeIfAbsent(capabilityCode, ignored -> new AtomicLong())
                .getAndIncrement();
        int offset = Math.floorMod(sequence, providers.size());
        List<ExternalProviderView> rotated = new ArrayList<>(providers.size());
        rotated.addAll(providers.subList(offset, providers.size()));
        rotated.addAll(providers.subList(0, offset));
        return rotated;
    }

    private static List<ExternalProviderView> weightedFirst(List<ExternalProviderView> providers) {
        int totalWeight = providers.stream().mapToInt(ExternalProviderView::weight).sum();
        int ticket = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        ExternalProviderView selected = providers.getFirst();
        for (ExternalProviderView provider : providers) {
            cumulative += provider.weight();
            if (ticket < cumulative) {
                selected = provider;
                break;
            }
        }
        List<ExternalProviderView> ordered = new ArrayList<>(providers.size());
        ordered.add(selected);
        for (ExternalProviderView provider : providers) {
            if (provider != selected) {
                ordered.add(provider);
            }
        }
        return ordered;
    }
}
