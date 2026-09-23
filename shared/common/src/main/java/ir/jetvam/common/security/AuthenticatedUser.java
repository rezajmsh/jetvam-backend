package ir.jetvam.common.security;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Carries the normalized identity extracted from an authenticated access token.
 * The model is framework-neutral and can be shared by every business module.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public record AuthenticatedUser(
        UUID userId,
        UUID partyId,
        String subject,
        Set<UserCategory> categories,
        Set<String> roles,
        Set<String> permissions,
        Map<String, Object> attributes
) implements Principal {

    public AuthenticatedUser {
        categories = categories == null ? Set.of() : Set.copyOf(categories);
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    @Override
    public String getName() {
        return subject;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean is(UserCategory category) {
        return categories.contains(category);
    }
}
