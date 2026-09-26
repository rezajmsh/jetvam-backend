package ir.jetvam.apps.uaa.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.List;

/**
 * Publishes the token components shared by standard and custom authorization grants.
 * A single generator keeps JWT customization and refresh-token behavior consistent.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Configuration(proxyBeanMethods = false)
public class OAuth2TokenConfiguration {

    @Bean
    JwtEncoder jetvamJwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    OAuth2TokenGenerator<OAuth2Token> jetvamTokenGenerator(
            JwtEncoder jwtEncoder,
            List<OAuth2TokenCustomizer<JwtEncodingContext>> jwtCustomizers
    ) {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        if (!jwtCustomizers.isEmpty()) {
            jwtGenerator.setJwtCustomizer(context ->
                    jwtCustomizers.forEach(customizer -> customizer.customize(context))
            );
        }
        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator,
                new OAuth2AccessTokenGenerator(),
                new OAuth2RefreshTokenGenerator()
        );
    }
}
