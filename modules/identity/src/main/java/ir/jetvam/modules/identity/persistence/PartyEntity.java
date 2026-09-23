package ir.jetvam.modules.identity.persistence;

import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import ir.jetvam.modules.identity.model.PartyStatus;
import ir.jetvam.modules.identity.model.PartyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stores the canonical identity root shared by customers, operators and merchants.
 * Type-specific identity fields are held in dedicated profile tables.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Entity
@Table(name = "iam_party")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "party_type", discriminatorType = DiscriminatorType.STRING, length = 30)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PartyEntity extends AbstractAuditableUuidEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PartyStatus status;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    protected PartyEntity(String displayName) {
        this.displayName = displayName;
        this.status = PartyStatus.ACTIVE;
    }

    @Transient
    public abstract PartyType getType();

    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void suspend() {
        this.status = PartyStatus.SUSPENDED;
    }
}
