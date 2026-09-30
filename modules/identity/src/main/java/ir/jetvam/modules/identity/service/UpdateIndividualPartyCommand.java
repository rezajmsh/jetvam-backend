package ir.jetvam.modules.identity.service;

import java.time.LocalDate;

/**
 * Carries an administrator-requested correction to canonical individual identity data.
 * Account roles and credentials remain outside this party-level command.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public record UpdateIndividualPartyCommand(
        String nationalCode,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String mobile
) {
}
