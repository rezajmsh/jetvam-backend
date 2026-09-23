package ir.jetvam.modules.identity.security;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.persistence.RoleEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapts a persisted Jetvam account to Spring Security authentication contracts.
 * Stable identifiers and access assignments remain available for JWT claims.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class IdentityUserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID partyId;
    private final String username;
    private final String password;
    private final UserAccountStatus status;
    private final Set<UserCategory> categories;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Set<GrantedAuthority> authorities;

    private IdentityUserPrincipal(
            UUID userId,
            UUID partyId,
            String username,
            String password,
            UserAccountStatus status,
            Set<UserCategory> categories,
            Set<String> roles,
            Set<String> permissions
    ) {
        this.userId = userId;
        this.partyId = partyId;
        this.username = username;
        this.password = password == null ? "{noop}<password-login-disabled>" : password;
        this.status = status;
        this.categories = Set.copyOf(categories);
        this.roles = Set.copyOf(roles);
        this.permissions = Set.copyOf(permissions);
        this.authorities = authorities(roles, permissions);
    }

    public static IdentityUserPrincipal from(UserAccountEntity account) {
        Set<String> roles = account.getRoles().stream()
                .map(RoleEntity::getCode)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> permissions = account.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toUnmodifiableSet());
        String login = account.getUsername() == null
                ? account.getParty().getId().toString()
                : account.getUsername();
        return new IdentityUserPrincipal(
                account.getId(),
                account.getParty().getId(),
                login,
                account.getPasswordHash(),
                account.getStatus(),
                account.getCategories(),
                roles,
                permissions
        );
    }

    public UUID userId() {
        return userId;
    }

    public UUID partyId() {
        return partyId;
    }

    public Set<UserCategory> categories() {
        return categories;
    }

    public Set<String> roles() {
        return roles;
    }

    public Set<String> permissions() {
        return permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserAccountStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserAccountStatus.ACTIVE;
    }

    private static Set<GrantedAuthority> authorities(Set<String> roles, Set<String> permissions) {
        Set<GrantedAuthority> result = new LinkedHashSet<>();
        roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).forEach(result::add);
        permissions.stream().map(SimpleGrantedAuthority::new).forEach(result::add);
        return Set.copyOf(result);
    }
}
