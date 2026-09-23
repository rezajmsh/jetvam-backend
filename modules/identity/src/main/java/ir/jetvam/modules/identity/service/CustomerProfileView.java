package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.identity.model.CustomerOnboardingStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents customer identity completion data safe for authenticated self-service APIs.
 * Mobile and national-code verification remain server-owned state.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record CustomerProfileView(
        UUID userId,
        UUID partyId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        CustomerOnboardingStatus onboardingStatus
) {
}
