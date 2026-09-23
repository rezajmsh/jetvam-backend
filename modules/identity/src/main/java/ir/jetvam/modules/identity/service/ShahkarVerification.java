package ir.jetvam.modules.identity.service;

/**
 * Carries the normalized Shahkar decision and its provider tracking reference.
 * Only a positive match permits customer account creation.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record ShahkarVerification(boolean matched, String trackingId) {
}
