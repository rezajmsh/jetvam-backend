package ir.jetvam.apps.uaa.authorization;

import ir.jetvam.apps.uaa.config.JetvamUaaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies the cohesive token and JDBC components owned by the UAA issuer.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
class AuthorizationServerComponentsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class))
            .withBean(JetvamUaaProperties.class, JetvamUaaProperties::new)
            .withUserConfiguration(
                    RequiredBeans.class,
                    JwtSigningKeyConfiguration.class,
                    AuthorizationServerComponentsConfiguration.class
            );

    @Test
    void publishesTokenAndPersistentAuthorizationComponents() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JwtEncoder.class);
            assertThat(context).hasSingleBean(OAuth2TokenCustomizer.class);
            assertThat(context).hasSingleBean(OAuth2TokenGenerator.class);
            assertThat(context).hasSingleBean(OAuth2AuthorizationService.class);
            assertThat(context).hasSingleBean(OAuth2AuthorizationConsentService.class);
        });
    }

    /**
     * Supplies the database and registered-client boundary needed by JDBC stores.
     *
     * @author reza jamshidi
     * @since 9/26/2026
     */
    @Configuration(proxyBeanMethods = false)
    static class RequiredBeans {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .addScript("org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql")
                    .addScript("org/springframework/security/oauth2/server/authorization/oauth2-authorization-consent-schema.sql")
                    .build();
        }

        @Bean
        RegisteredClientRepository registeredClientRepository() {
            return mock(RegisteredClientRepository.class);
        }
    }
}
