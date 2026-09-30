package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.identity.model.IdentityVerificationStatus;
import ir.jetvam.modules.identity.model.MobileVerificationStatus;
import ir.jetvam.modules.identity.model.ShahkarStatus;

import java.time.LocalDate;
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

    public record PersonalInformation(
            String bankCardNumber,
            String landline,
            String postalCode,
            String address
    ) {
    }

    public record IdentityInformation(
            String nationalCode,
            String firstName,
            String lastName,
            LocalDate birthDate
    ) {
    }

    public record ProfileView(
            UUID customerPartyId,
            MobileVerificationStatus mobileVerificationStatus,
            ShahkarStatus shahkarStatus,
            IdentityVerificationStatus identityVerificationStatus,
            boolean identityInformationComplete,
            boolean personalInformationComplete,
            boolean readyForApplication,
            IdentityInformation identityInformation,
            PersonalInformation personalInformation
    ) {
    }
}
