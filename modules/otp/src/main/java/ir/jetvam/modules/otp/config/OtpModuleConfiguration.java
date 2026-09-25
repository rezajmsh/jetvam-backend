package ir.jetvam.modules.otp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Activates configuration properties owned by the reusable OTP module.
 * Component and repository discovery remain controlled by each hosting application.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OtpProperties.class)
public class OtpModuleConfiguration {
}
