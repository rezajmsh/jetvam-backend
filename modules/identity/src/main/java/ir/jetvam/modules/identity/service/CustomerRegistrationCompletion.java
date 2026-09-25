package ir.jetvam.modules.identity.service;

/**
 * Carries the committed local outcome of customer-registration completion.
 * A Shahkar mismatch is persisted first and translated to an API error outside the transaction.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record CustomerRegistrationCompletion(
        boolean matched,
        CustomerRegistrationResult registration
) {

    public static CustomerRegistrationCompletion rejected() {
        return new CustomerRegistrationCompletion(false, null);
    }

    public static CustomerRegistrationCompletion accepted(CustomerRegistrationResult registration) {
        return new CustomerRegistrationCompletion(true, registration);
    }
}
