package ir.jetvam.modules.integration.routing;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies provider isolation, open duration and single half-open trial behavior.
 * Circuit state is deterministic through a controllable test clock.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
class ProviderCircuitRegistryTest {

    @Test
    void opensAfterThresholdAndRecoversThroughOneHalfOpenTrial() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-24T00:00:00Z"));
        ProviderCircuitRegistry registry = new ProviderCircuitRegistry(clock);

        registry.recordFailure("SHAHKAR_VERIFY", "A", 2, 30);
        assertThat(registry.tryAcquire("SHAHKAR_VERIFY", "A")).isTrue();

        registry.recordFailure("SHAHKAR_VERIFY", "A", 2, 30);
        assertThat(registry.tryAcquire("SHAHKAR_VERIFY", "A")).isFalse();
        assertThat(registry.tryAcquire("SHAHKAR_VERIFY", "B")).isTrue();

        clock.advance(Duration.ofSeconds(31));
        assertThat(registry.tryAcquire("SHAHKAR_VERIFY", "A")).isTrue();
        assertThat(registry.tryAcquire("SHAHKAR_VERIFY", "A")).isFalse();

        registry.recordSuccess("SHAHKAR_VERIFY", "A");
        assertThat(registry.tryAcquire("SHAHKAR_VERIFY", "A")).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
