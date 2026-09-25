package ir.jetvam.apps.uaa.grant.password;

import ir.jetvam.common.exception.RateLimitExceededException;
import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.infra.security.SecurityClaims;
import ir.jetvam.modules.identity.security.IdentityUserPrincipal;
import ir.jetvam.modules.identity.service.PasswordAuthenticationResult;
import ir.jetvam.modules.identity.service.PasswordUserAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Exchanges password credentials and a policy-required OTP for OAuth tokens.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@Component
@RequiredArgsConstructor
public class PasswordGrantAuthenticationProvider implements AuthenticationProvider {

    private final PasswordUserAuthenticationService passwordAuthenticationService;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

    @Override
    public Authentication authenticate(Authentication authentication) {
        PasswordGrantAuthenticationToken passwordGrant = (PasswordGrantAuthenticationToken) authentication;
        OAuth2ClientAuthenticationToken clientPrincipal = authenticatedClient(passwordGrant);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
        if (registeredClient == null
                || !registeredClient.getAuthorizationGrantTypes().contains(PasswordGrantConstants.GRANT_TYPE)) {
            throw oauthError(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                    "Client is not allowed to use the password grant");
        }
        Set<String> authorizedScopes = authorizedScopes(passwordGrant, registeredClient);
        PasswordAuthenticationResult authenticationResult;
        try {
            authenticationResult = passwordAuthenticationService.authenticate(
                    passwordGrant.username(),
                    passwordGrant.password(),
                    passwordGrant.challengeId(),
                    passwordGrant.otp()
            );
        } catch (RateLimitExceededException exception) {
            throw oauthError(OAuth2ErrorCodes.TEMPORARILY_UNAVAILABLE,
                    "A new one-time password cannot be issued yet");
        } catch (ValidationException | IllegalArgumentException exception) {
            throw oauthError(OAuth2ErrorCodes.INVALID_GRANT,
                    "Username, password or second factor is invalid");
        }
        if (authenticationResult.requiresSecondFactor()) {
            throw new PasswordSecondFactorRequiredException(authenticationResult.challenge());
        }
        IdentityUserPrincipal user = authenticationResult.principal();
        Authentication userAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities()
        );
        Authentication persistedUserAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                user.getUsername(), null, user.getAuthorities()
        );

        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(user.getUsername())
                .authorizationGrantType(PasswordGrantConstants.GRANT_TYPE)
                .authorizedScopes(authorizedScopes)
                .attribute(Principal.class.getName(), persistedUserAuthentication)
                .attribute(SecurityClaims.USER_ID, user.userId().toString())
                .attribute(SecurityClaims.PARTY_ID, user.partyId().toString())
                .attribute(SecurityClaims.CATEGORIES,
                        new ArrayList<>(user.categories().stream().map(Enum::name).toList()))
                .attribute(SecurityClaims.ROLES, new ArrayList<>(user.roles()))
                .attribute(SecurityClaims.PERMISSIONS, new ArrayList<>(user.permissions()))
                .attribute(SecurityClaims.AUTHENTICATION_METHODS, new ArrayList<>(passwordGrant.challengeId() == null
                        ? Set.of("pwd")
                        : Set.of("pwd", "otp")));
        OAuth2Authorization authorization = authorizationBuilder.build();

        OAuth2TokenContext accessContext = tokenContext(
                registeredClient, userAuthentication, passwordGrant,
                authorizedScopes, authorization, OAuth2TokenType.ACCESS_TOKEN
        );
        OAuth2Token generatedAccessToken = tokenGenerator.generate(accessContext);
        if (generatedAccessToken == null) {
            throw oauthError(OAuth2ErrorCodes.SERVER_ERROR,
                    "Token generator did not create an access token");
        }
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(),
                generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(),
                authorizedScopes
        );
        authorizationBuilder.token(accessToken, metadata -> addClaims(metadata, generatedAccessToken));
        authorization = authorizationBuilder.build();

        OAuth2RefreshToken refreshToken = null;
        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            OAuth2TokenContext refreshContext = tokenContext(
                    registeredClient, userAuthentication, passwordGrant,
                    authorizedScopes, authorization, OAuth2TokenType.REFRESH_TOKEN
            );
            OAuth2Token generatedRefreshToken = tokenGenerator.generate(refreshContext);
            if (generatedRefreshToken instanceof OAuth2RefreshToken generated) {
                refreshToken = generated;
                authorizationBuilder.token(refreshToken);
            }
        }

        authorizationService.save(authorizationBuilder.build());
        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient, clientPrincipal, accessToken, refreshToken, Map.of()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PasswordGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static OAuth2ClientAuthenticationToken authenticatedClient(PasswordGrantAuthenticationToken grant) {
        Object principal = grant.getPrincipal();
        if (principal instanceof OAuth2ClientAuthenticationToken client && client.isAuthenticated()) {
            return client;
        }
        throw oauthError(OAuth2ErrorCodes.INVALID_CLIENT, "OAuth client authentication is required");
    }

    private static Set<String> authorizedScopes(
            PasswordGrantAuthenticationToken grant,
            RegisteredClient registeredClient
    ) {
        Set<String> requested = grant.requestedScopes();
        if (!registeredClient.getScopes().containsAll(requested)) {
            throw oauthError(OAuth2ErrorCodes.INVALID_SCOPE, "Requested scope is not allowed for this client");
        }
        if (!requested.isEmpty()) {
            return requested;
        }
        Set<String> defaults = new LinkedHashSet<>(registeredClient.getScopes());
        defaults.remove("openid");
        defaults.remove("offline_access");
        return Set.copyOf(defaults);
    }

    private static OAuth2TokenContext tokenContext(
            RegisteredClient registeredClient,
            Authentication userAuthentication,
            PasswordGrantAuthenticationToken grant,
            Set<String> scopes,
            OAuth2Authorization authorization,
            OAuth2TokenType tokenType
    ) {
        return DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorization(authorization)
                .authorizedScopes(scopes)
                .tokenType(tokenType)
                .authorizationGrantType(PasswordGrantConstants.GRANT_TYPE)
                .authorizationGrant(grant)
                .build();
    }

    private static void addClaims(Map<String, Object> metadata, OAuth2Token generatedToken) {
        if (generatedToken instanceof ClaimAccessor claimAccessor) {
            metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claimAccessor.getClaims());
        }
    }

    private static OAuth2AuthenticationException oauthError(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
    }
}
