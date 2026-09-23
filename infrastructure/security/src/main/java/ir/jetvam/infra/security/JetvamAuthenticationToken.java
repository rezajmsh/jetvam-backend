package ir.jetvam.infra.security;

import ir.jetvam.common.security.AuthenticatedUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * Represents a verified JWT with a framework-neutral Jetvam principal.
 * It retains the source token while exposing normalized user information.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class JetvamAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedUser principal;
    private final Jwt token;

    public JetvamAuthenticationToken(
            AuthenticatedUser principal,
            Jwt token,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return principal;
    }

    @Override
    public Jwt getCredentials() {
        return token;
    }

    public Jwt token() {
        return token;
    }
}
