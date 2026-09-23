package ir.jetvam.modules.identity.persistence;

import ir.jetvam.modules.identity.model.PartyType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Protects the joined Party hierarchy and its discriminator contract.
 * Customer profiles remain roles attached to a Party rather than Party subtypes.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class PartyInheritanceMappingTest {

    @Test
    void mapsIndividualAndOrganizationAsJoinedPartySubtypes() {
        Inheritance inheritance = PartyEntity.class.getAnnotation(Inheritance.class);
        DiscriminatorColumn discriminator = PartyEntity.class.getAnnotation(DiscriminatorColumn.class);

        assertThat(Modifier.isAbstract(PartyEntity.class.getModifiers())).isTrue();
        assertThat(inheritance.strategy()).isEqualTo(InheritanceType.JOINED);
        assertThat(discriminator.name()).isEqualTo("party_type");
        assertThat(IndividualPartyEntity.class).isAssignableTo(PartyEntity.class);
        assertThat(OrganizationPartyEntity.class).isAssignableTo(PartyEntity.class);
        assertThat(IndividualPartyEntity.class.getAnnotation(DiscriminatorValue.class).value())
                .isEqualTo(PartyType.INDIVIDUAL.name());
        assertThat(OrganizationPartyEntity.class.getAnnotation(DiscriminatorValue.class).value())
                .isEqualTo(PartyType.ORGANIZATION.name());
        assertThat(PartyEntity.class.isAssignableFrom(CustomerProfileEntity.class)).isFalse();
    }
}
