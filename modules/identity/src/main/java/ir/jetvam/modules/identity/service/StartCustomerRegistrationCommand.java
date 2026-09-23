package ir.jetvam.modules.identity.service;

/**
 * Starts customer onboarding with the two identifiers required for Shahkar.
 * Both values are normalized before the OTP challenge is persisted.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record StartCustomerRegistrationCommand(String mobile, String nationalCode) {
}
