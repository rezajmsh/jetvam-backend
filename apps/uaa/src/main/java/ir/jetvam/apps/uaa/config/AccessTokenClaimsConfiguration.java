package ir.jetvam.apps.uaa.config;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.infra.security.SecurityClaims;
import ir.jetvam.modules.identity.IdentityRoles;
import ir.jetvam.modules.identity.security.IdentityUserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Set;

/**
 * Adds Jetvam identity, role and permission claims to issued access tokens.
 * Resource servers consume the same stable claim contract through infra-security.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Configuration(proxyBeanMethods = false)
public class AccessTokenClaimsConfiguration {

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

    private static boolean hasPersistedUserClaims(OAuth2Authorization authorization) {
        return authorization != null && authorization.getAttribute(SecurityClaims.USER_ID) != null;
    }
}
