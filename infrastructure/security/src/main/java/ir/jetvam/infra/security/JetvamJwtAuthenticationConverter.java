package ir.jetvam.infra.security;

import ir.jetvam.common.security.AuthenticatedUser;
import ir.jetvam.common.security.UserCategory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Converts verified JWT claims into the shared Jetvam user representation.
 * Roles, permissions and OAuth scopes are synchronized with Spring authorities.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class JetvamJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<String> roles = strings(jwt.getClaims().get(SecurityClaims.ROLES));
        Set<String> permissions = strings(jwt.getClaims().get(SecurityClaims.PERMISSIONS));
        Set<UserCategory> categories = strings(jwt.getClaims().get(SecurityClaims.CATEGORIES)).stream()
                .map(String::toUpperCase)
                .map(UserCategory::valueOf)
                .collect(Collectors.toUnmodifiableSet());

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopeAuthorities(jwt));
        roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).forEach(authorities::add);
        permissions.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);

        var principal = new AuthenticatedUser(
                uuid(jwt.getClaimAsString(SecurityClaims.USER_ID)),
                uuid(jwt.getClaimAsString(SecurityClaims.PARTY_ID)),
                jwt.getSubject(),
                categories,
                roles,
                permissions,
                selectedAttributes(jwt)
        );
        return new JetvamAuthenticationToken(principal, jwt, authorities);
    }

    private Collection<GrantedAuthority> scopeAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = scopeConverter.convert(jwt);
        return authorities == null ? Set.of() : authorities;
    }

    private static Set<String> strings(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
        }
        if (value instanceof String text && !text.isBlank()) {
            return Set.of(text.split("[ ,]+"));
        }
        return Set.of();
    }

    private static UUID uuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private static Map<String, Object> selectedAttributes(Jwt jwt) {
        return Map.of(
                "issuer", jwt.getIssuer() == null ? "" : jwt.getIssuer().toString(),
                "clientId", jwt.getClaimAsString("client_id") == null ? "" : jwt.getClaimAsString("client_id")
        );
    }
}
