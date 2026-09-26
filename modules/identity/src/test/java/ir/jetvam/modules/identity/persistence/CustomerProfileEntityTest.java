package ir.jetvam.modules.identity.persistence;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies typed reusable customer profile data and independent section revisions.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class CustomerProfileEntityTest {

    @Test
    void updatesPersonalAndEmploymentSectionsWithoutJson() {
        CustomerProfileEntity profile = new CustomerProfileEntity(new IndividualPartyEntity(
                "Customer", "0013546789", "Reza", "Jamshidi", LocalDate.of(1990, 1, 1), "09121234567"
        ));
        UUID documentId = UUID.randomUUID();

        profile.updatePersonalInformation(
                "6037991234567890", "02112345678", "1234567890", "Tehran"
        );
        profile.updateEmploymentInformation(
                "BACHELOR", "EMPLOYEE", BigDecimal.valueOf(500_000_000), List.of(documentId)
        );
        profile.updatePersonalInformation(
                "6037991234567890", "02187654321", "1234567890", "Tehran"
        );

        assertThat(profile.getPersonalInformationRevision()).isEqualTo(2);
        assertThat(profile.getEmploymentInformationRevision()).isEqualTo(1);
        assertThat(profile.getLandline()).isEqualTo("02187654321");
        assertThat(profile.getMonthlyIncome()).isEqualByComparingTo("500000000");
        assertThat(profile.getEmploymentDocumentIds()).containsExactly(documentId);
    }
}
