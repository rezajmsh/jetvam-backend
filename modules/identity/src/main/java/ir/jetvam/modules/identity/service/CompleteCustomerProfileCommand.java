package ir.jetvam.modules.identity.service;

import java.time.LocalDate;

/**
 * Carries personal data supplied after mobile and Shahkar verification.
 * Completion is intentionally separate from creation of the login account.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record CompleteCustomerProfileCommand(String firstName, String lastName, LocalDate birthDate) {
}
