package ir.jetvam.modules.identity.persistence;

import ir.jetvam.modules.identity.model.PartyType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stores legal identity data for merchant and organizational parties.
 * Operational merchant details remain in the merchant business module.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Entity
@Table(name = "iam_organization_party")
@DiscriminatorValue("ORGANIZATION")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationPartyEntity extends PartyEntity {

    @Column(name = "national_id", nullable = false, unique = true, length = 20)
    private String nationalId;

    @Column(name = "legal_name", nullable = false, length = 250)
    private String legalName;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    public OrganizationPartyEntity(
            String nationalId,
            String legalName,
            String registrationNumber
    ) {
        super(legalName);
        this.nationalId = nationalId;
        this.legalName = legalName;
        this.registrationNumber = registrationNumber;
    }

    @Override
    @Transient
    public PartyType getType() {
        return PartyType.ORGANIZATION;
    }
}
