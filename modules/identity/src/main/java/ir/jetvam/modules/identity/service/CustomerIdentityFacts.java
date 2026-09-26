package ir.jetvam.modules.identity.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Exposes verified customer facts to trusted business modules without leaking identity entities.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public record CustomerIdentityFacts(
        UUID partyId,
        String nationalCode,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String mobile
) {
}
