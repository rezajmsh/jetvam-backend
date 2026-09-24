package ir.jetvam.infra.security;

import ir.jetvam.common.security.AuthenticatedUser;
import ir.jetvam.common.security.UserCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies JWT claim mapping and static access to the current Jetvam user.
 * The test protects synchronization between token claims and Spring Security.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
class JetvamJwtAuthenticationConverterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mapsIdentityClaimsRolesPermissionsAndScopes() {
        UUID userId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("operator")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim(SecurityClaims.USER_ID, userId.toString())
                .claim(SecurityClaims.PARTY_ID, partyId.toString())
                .claim(SecurityClaims.CATEGORIES, List.of("OPERATOR"))
                .claim(SecurityClaims.ROLES, List.of("SYSTEM_ADMIN"))
                .claim(SecurityClaims.PERMISSIONS, List.of("identity:user:read"))
                .claim(SecurityClaims.AUTHENTICATION_METHODS, List.of("pwd", "otp"))
                .claim("scope", "jetvam.api")
                .build();

        var authentication = new JetvamJwtAuthenticationConverter().convert(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AuthenticatedUser current = CurrentUser.required();
        assertThat(current.userId()).isEqualTo(userId);
        assertThat(current.partyId()).isEqualTo(partyId);
        assertThat(current.categories()).containsExactly(UserCategory.OPERATOR);
        assertThat(CurrentUser.hasRole("SYSTEM_ADMIN")).isTrue();
        assertThat(CurrentUser.hasPermission("identity:user:read")).isTrue();
        assertThat(current.attributes().get(SecurityClaims.AUTHENTICATION_METHODS))
                .isEqualTo(Set.of("pwd", "otp"));
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_SYSTEM_ADMIN", "identity:user:read", "SCOPE_jetvam.api");
    }
}
