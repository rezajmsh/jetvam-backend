package ir.jetvam.infra.core.config;

import ir.jetvam.common.time.ClockTimeProvider;
import ir.jetvam.common.time.JetvamClocks;
import ir.jetvam.common.time.TimeProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/** Central auto-configuration for runtime primitives shared by all applications.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@AutoConfiguration
public class JetvamCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock jetvamClock() {
        return JetvamClocks.systemUtc();
    }

    @Bean
    @ConditionalOnMissingBean
    TimeProvider jetvamTimeProvider(Clock clock) {
        return new ClockTimeProvider(clock);
    }
}
