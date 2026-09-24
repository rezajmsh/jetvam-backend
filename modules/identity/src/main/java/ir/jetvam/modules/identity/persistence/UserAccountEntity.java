package ir.jetvam.modules.identity.persistence;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.common.validation.Preconditions;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stores authentication credentials and access assignments for a party.
 * Personal identity remains outside the account to avoid duplication across roles.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Entity
@Table(name = "iam_user_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccountEntity extends AbstractAuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    private PartyEntity party;

    @Column(name = "username", unique = true, length = 150)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "authentication_mobile", length = 11)
    private String authenticationMobile;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_authentication_method", nullable = false, length = 30)
    private AuthenticationMethod primaryAuthenticationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserAccountStatus status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "iam_user_category", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private Set<UserCategory> categories = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "iam_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new LinkedHashSet<>();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public UserAccountEntity(
            PartyEntity party,
            String username,
            String passwordHash,
            Set<UserCategory> categories,
            Set<RoleEntity> roles
    ) {
        this(
                party,
                username,
                passwordHash,
                party instanceof IndividualPartyEntity individual ? individual.getMobile() : null,
                AuthenticationMethod.PASSWORD,
                categories,
                roles
        );
    }

    public UserAccountEntity(
            PartyEntity party,
            String username,
            String passwordHash,
            String authenticationMobile,
            Set<UserCategory> categories,
            Set<RoleEntity> roles
    ) {
        this(party, username, passwordHash, authenticationMobile,
                AuthenticationMethod.PASSWORD, categories, roles);
    }

    public UserAccountEntity(
            PartyEntity party,
            String username,
            String passwordHash,
            AuthenticationMethod primaryAuthenticationMethod,
            Set<UserCategory> categories,
            Set<RoleEntity> roles
    ) {
        this(party, username, passwordHash, null, primaryAuthenticationMethod, categories, roles);
    }

    private UserAccountEntity(
            PartyEntity party,
            String username,
            String passwordHash,
            String authenticationMobile,
            AuthenticationMethod primaryAuthenticationMethod,
            Set<UserCategory> categories,
            Set<RoleEntity> roles
    ) {
        this.party = Preconditions.requireNonNull(party, "party");
        this.primaryAuthenticationMethod = Preconditions.requireNonNull(
                primaryAuthenticationMethod,
                "primaryAuthenticationMethod"
        );
        if (primaryAuthenticationMethod == AuthenticationMethod.PASSWORD) {
            this.username = Preconditions.requireText(username, "username");
            this.passwordHash = Preconditions.requireText(passwordHash, "passwordHash");
            this.authenticationMobile = authenticationMobile;
        } else {
            Preconditions.require(username == null && passwordHash == null && authenticationMobile == null,
                    "OTP accounts must not contain username or password credentials");
        }
        this.categories.addAll(Preconditions.requireNonNull(categories, "categories"));
        this.roles.addAll(Preconditions.requireNonNull(roles, "roles"));
        this.status = UserAccountStatus.ACTIVE;
    }

    public void changeStatus(UserAccountStatus status) {
        this.status = status;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void recordSuccessfulLogin(Instant loginAt) {
        this.lastLoginAt = loginAt;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
}
