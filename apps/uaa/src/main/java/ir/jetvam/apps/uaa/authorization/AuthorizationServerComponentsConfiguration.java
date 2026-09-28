package ir.jetvam.apps.uaa.authorization;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.infra.security.SecurityClaims;
import ir.jetvam.modules.identity.IdentityRoles;
import ir.jetvam.modules.identity.security.IdentityUserPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import java.util.List;
import java.util.Set;

/**
 * Groups token generation, Jetvam claims and JDBC authorization persistence.
 * These components are internal implementation details of the UAA issuer.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
@Configuration(proxyBeanMethods = false)
public class AuthorizationServerComponentsConfiguration {

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

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jetvamAccessTokenCustomizer() {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                return;
            }
            Object principal = context.getPrincipal().getPrincipal();
            if (principal instanceof IdentityUserPrincipal user) {
                context.getClaims().claim(SecurityClaims.USER_ID, user.userId().toString());
                context.getClaims().claim(SecurityClaims.PARTY_ID, user.partyId().toString());
                context.getClaims().claim(
                        SecurityClaims.CATEGORIES,
                        user.categories().stream().map(Enum::name).toList()
                );
                context.getClaims().claim(SecurityClaims.ROLES, user.roles());
                context.getClaims().claim(SecurityClaims.PERMISSIONS, user.permissions());
                context.getClaims().claim(SecurityClaims.AUTHENTICATION_VERSION, user.authenticationVersion());
            } else if (hasPersistedUserClaims(context.getAuthorization())) {
                OAuth2Authorization authorization = context.getAuthorization();
                context.getClaims().claim(SecurityClaims.USER_ID,
                        authorization.getAttribute(SecurityClaims.USER_ID));
                context.getClaims().claim(SecurityClaims.PARTY_ID,
                        authorization.getAttribute(SecurityClaims.PARTY_ID));
                context.getClaims().claim(SecurityClaims.CATEGORIES,
                        authorization.getAttribute(SecurityClaims.CATEGORIES));
                context.getClaims().claim(SecurityClaims.ROLES,
                        authorization.getAttribute(SecurityClaims.ROLES));
                context.getClaims().claim(SecurityClaims.PERMISSIONS,
                        authorization.getAttribute(SecurityClaims.PERMISSIONS));
                context.getClaims().claim(SecurityClaims.AUTHENTICATION_VERSION,
                        authorization.getAttribute(SecurityClaims.AUTHENTICATION_VERSION));
            } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                context.getClaims().claim(SecurityClaims.CATEGORIES, Set.of(UserCategory.SERVICE.name()));
                context.getClaims().claim(SecurityClaims.ROLES, Set.of(IdentityRoles.SERVICE));
            }
            if (context.getAuthorization() != null) {
                Object authenticationMethods = context.getAuthorization()
                        .getAttribute(SecurityClaims.AUTHENTICATION_METHODS);
                if (authenticationMethods != null) {
                    context.getClaims().claim(SecurityClaims.AUTHENTICATION_METHODS, authenticationMethods);
                }
            }
        };
    }

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

    private static boolean hasPersistedUserClaims(OAuth2Authorization authorization) {
        return authorization != null && authorization.getAttribute(SecurityClaims.USER_ID) != null;
    }
}
