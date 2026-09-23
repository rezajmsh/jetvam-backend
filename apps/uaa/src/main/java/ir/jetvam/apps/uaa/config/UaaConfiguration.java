package ir.jetvam.apps.uaa.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Activates typed UAA settings used for OAuth client and token configuration.
 * Keeping binding centralized makes deployment overrides explicit and validated.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JetvamUaaProperties.class)
public class UaaConfiguration {
}
