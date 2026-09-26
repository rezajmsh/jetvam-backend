package ir.jetvam.modules.identity.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Defines type-safe customer profile commands and views shared with consuming workflows.
 * Personal and employment information are represented as structured values rather than JSON.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class CustomerProfileModels {

    private CustomerProfileModels() {
    }

    public record UpdatePersonalInformation(
            String bankCardNumber,
            String landline,
            String postalCode,
            String address
    ) {
    }

    public record UpdateEmploymentInformation(
            String educationCode,
            String employmentCode,
            BigDecimal monthlyIncome,
            List<UUID> documentIds
    ) {
        public UpdateEmploymentInformation {
            documentIds = documentIds == null ? List.of() : List.copyOf(documentIds);
        }
    }

    public record PersonalInformation(
            String bankCardNumber,
            String landline,
            String postalCode,
            String address,
            long revision
    ) {
    }

    public record EmploymentInformation(
            String educationCode,
            String employmentCode,
            BigDecimal monthlyIncome,
            List<UUID> documentIds,
            long revision
    ) {
        public EmploymentInformation {
            documentIds = List.copyOf(documentIds);
        }
    }

    public record ProfileView(
            UUID customerPartyId,
            PersonalInformation personalInformation,
            EmploymentInformation employmentInformation
    ) {
    }
}
