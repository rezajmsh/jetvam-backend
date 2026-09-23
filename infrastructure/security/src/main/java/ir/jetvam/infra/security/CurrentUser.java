package ir.jetvam.infra.security;

import ir.jetvam.common.security.AuthenticatedUser;
import ir.jetvam.common.security.UserCategory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides static, read-only access to the user held by Spring Security.
 * Business code can query the current identity without injecting security services.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> optional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof AuthenticatedUser user
                ? Optional.of(user)
                : Optional.empty();
    }

    public static AuthenticatedUser required() {
        return optional().orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                "No authenticated Jetvam user is available"
        ));
    }

    public static UUID userId() {
        UUID userId = required().userId();
        if (userId == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "The authenticated principal represents a service, not a user"
            );
        }
        return userId;
    }

    public static Optional<UUID> optionalUserId() {
        return optional().map(AuthenticatedUser::userId);
    }

    public static Optional<UUID> partyId() {
        return optional().map(AuthenticatedUser::partyId);
    }

    public static boolean hasRole(String role) {
        return optional().map(user -> user.hasRole(role)).orElse(false);
    }

    public static boolean hasPermission(String permission) {
        return optional().map(user -> user.hasPermission(permission)).orElse(false);
    }

    public static boolean is(UserCategory category) {
        return optional().map(user -> user.is(category)).orElse(false);
    }
}
