package ir.jetvam.infra.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;

/**
 * Records HTTP, repository and audit metrics with bounded dimensions.
 * Feature flags control metric families independently.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@RequiredArgsConstructor
public class InfrastructureMetrics {

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public void increment(String name, Iterable<Tag> tags) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            meterRegistry.counter(name, tags).increment();
        }
    }

    public void record(String name, Duration duration, Iterable<Tag> tags) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            Timer.builder(name)
                    .tags(tags)
                    .publishPercentileHistogram()
                    .register(meterRegistry)
                    .record(duration);
        }
    }
}
