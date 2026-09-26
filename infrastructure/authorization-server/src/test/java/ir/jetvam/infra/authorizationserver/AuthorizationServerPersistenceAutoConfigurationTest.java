package ir.jetvam.infra.authorizationserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies conditional creation of database-backed authorization-server stores.
 * The stores must only activate for an explicitly enabled UAA deployment.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class AuthorizationServerPersistenceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JdbcTemplateAutoConfiguration.class,
                    AuthorizationServerPersistenceAutoConfiguration.class
            ))
            .withUserConfiguration(RequiredBeans.class);

    @Test
    void createsStoresWhenEnabled() {
        contextRunner
                .withPropertyValues("jetvam.security.authorization-server.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(OAuth2AuthorizationService.class);
                    assertThat(context).hasSingleBean(OAuth2AuthorizationConsentService.class);
                });
    }

    @Test
    void remainsInactiveByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(OAuth2AuthorizationService.class);
            assertThat(context).doesNotHaveBean(OAuth2AuthorizationConsentService.class);
        });
    }

    /**
     * Supplies the collaborating infrastructure beans needed by the auto-configuration.
     * Mocks keep the test focused on conditional bean registration.
     *
     * @author reza jamshidi
     * @since 9/22/2026
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
