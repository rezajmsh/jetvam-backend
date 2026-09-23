package ir.jetvam.modules.identity.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Registers credential encoding required by identity application services.
 * Entity and repository discovery remain centralized in infra-persistence.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentitySecurityProperties.class)
public class IdentityModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean
    PasswordEncoder jetvamPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
