package ir.jetvam.infra.core.config;

import ir.jetvam.common.time.TimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the behavior of jetvam core auto configuration.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class JetvamCoreAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JetvamCoreAutoConfiguration.class));

    @Test
    void suppliesOneApplicationWideClockAndTimeProvider() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(TimeProvider.class);
        });
    }

    @Test
    void timeProviderUsesAnApplicationSuppliedClock() {
        var expected = Instant.parse("2026-09-21T10:00:00Z");

        contextRunner
                .withUserConfiguration(FixedClockConfiguration.class)
                .run(context -> assertThat(context.getBean(TimeProvider.class).now()).isEqualTo(expected));
    }

    /**
     * Supplies a deterministic clock for auto-configuration tests.
     * It verifies that application beans override infrastructure defaults.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Configuration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
