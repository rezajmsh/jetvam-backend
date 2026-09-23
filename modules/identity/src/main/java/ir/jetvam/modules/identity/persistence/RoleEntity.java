package ir.jetvam.modules.identity.persistence;

import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Defines an assignable security role and its granular permissions.
 * Roles are data-driven so operator access can evolve without token code changes.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Entity
@Table(name = "iam_role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleEntity extends AbstractAuditableUuidEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "iam_role_permission", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", nullable = false, length = 150)
    private Set<String> permissions = new LinkedHashSet<>();
}
