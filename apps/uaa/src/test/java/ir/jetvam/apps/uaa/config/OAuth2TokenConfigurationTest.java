package ir.jetvam.apps.uaa.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that custom grants can inject the shared authorization-server token generator.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class OAuth2TokenConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    UaaConfiguration.class,
                    JwtSigningKeyConfiguration.class,
                    AccessTokenClaimsConfiguration.class,
                    OAuth2TokenConfiguration.class
            );

    @Test
    void publishesJwtEncoderCustomizerAndSharedTokenGenerator() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JwtEncoder.class);
            assertThat(context).hasSingleBean(OAuth2TokenCustomizer.class);
            assertThat(context).hasSingleBean(OAuth2TokenGenerator.class);
        });
    }
}
