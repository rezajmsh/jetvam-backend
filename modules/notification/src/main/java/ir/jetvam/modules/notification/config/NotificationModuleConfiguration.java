package ir.jetvam.modules.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activates typed notification settings and scheduled outbox dispatch support.
 * Actual polling remains guarded by the dispatcher-enabled property.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationModuleConfiguration {
}
