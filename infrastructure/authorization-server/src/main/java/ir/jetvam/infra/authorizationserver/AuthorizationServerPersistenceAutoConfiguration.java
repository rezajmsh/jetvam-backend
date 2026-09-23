package ir.jetvam.infra.authorizationserver;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Auto-configures persistent authorization and consent stores for the UAA app.
 * Database infrastructure and JDBC observability remain centrally supplied.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@AutoConfiguration
@ConditionalOnClass(JdbcOAuth2AuthorizationService.class)
@ConditionalOnProperty(prefix = "jetvam.security.authorization-server", name = "enabled", havingValue = "true")
@ConditionalOnBean({JdbcOperations.class, RegisteredClientRepository.class})
public class AuthorizationServerPersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    OAuth2AuthorizationService authorizationService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
    }
}
