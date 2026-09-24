package ir.jetvam.modules.integration.routing;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintains an independent in-memory circuit for each capability and provider pair.
 * Persisted thresholds are applied on every failure so policy changes need no restart.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Component
public class ProviderCircuitRegistry {

    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();
    private final Clock clock;

    public ProviderCircuitRegistry() {
        this(Clock.systemUTC());
    }

    ProviderCircuitRegistry(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(String capabilityCode, String providerCode) {
        CircuitState state = circuits.computeIfAbsent(key(capabilityCode, providerCode), ignored -> new CircuitState());
        Instant openUntil = state.openUntil;
        if (openUntil == null) {
            return true;
        }
        Instant now = clock.instant();
        if (openUntil.isAfter(now)) {
            return false;
        }
        return state.halfOpenTrial.compareAndSet(false, true);
    }

    public void recordSuccess(String capabilityCode, String providerCode) {
        CircuitState state = circuits.computeIfAbsent(key(capabilityCode, providerCode), ignored -> new CircuitState());
        state.failures.set(0);
        state.openUntil = null;
        state.halfOpenTrial.set(false);
    }

    public void recordFailure(
            String capabilityCode,
            String providerCode,
            int failureThreshold,
            long openDurationSeconds
    ) {
        CircuitState state = circuits.computeIfAbsent(key(capabilityCode, providerCode), ignored -> new CircuitState());
        int failures = state.failures.incrementAndGet();
        if (state.halfOpenTrial.get() || failures >= failureThreshold) {
            state.openUntil = clock.instant().plusSeconds(openDurationSeconds);
            state.halfOpenTrial.set(false);
        }
    }

    public void reset(String capabilityCode, String providerCode) {
        circuits.remove(key(capabilityCode, providerCode));
    }

    public List<ProviderCircuitView> snapshots() {
        Instant now = clock.instant();
        return circuits.entrySet().stream()
                .map(entry -> view(entry.getKey(), entry.getValue(), now))
                .sorted(Comparator.comparing(ProviderCircuitView::capabilityCode)
                        .thenComparing(ProviderCircuitView::providerCode))
                .toList();
    }

    private static ProviderCircuitView view(String key, CircuitState state, Instant now) {
        int separator = key.indexOf(':');
        Instant openUntil = state.openUntil;
        if (openUntil != null && !openUntil.isAfter(now) && !state.halfOpenTrial.get()) {
            openUntil = null;
        }
        return new ProviderCircuitView(
                key.substring(0, separator), key.substring(separator + 1), state.failures.get(),
                openUntil, state.halfOpenTrial.get()
        );
    }

    private static String key(String capabilityCode, String providerCode) {
        return capabilityCode.strip().toUpperCase() + ":" + providerCode.strip().toUpperCase();
    }

    private static final class CircuitState {
        private final AtomicInteger failures = new AtomicInteger();
        private final AtomicBoolean halfOpenTrial = new AtomicBoolean();
        private volatile Instant openUntil;
    }
}
