package ir.jetvam.modules.integration.shahkar;

/**
 * Carries the canonical Shahkar decision and provider tracking reference.
 * Consumers do not depend on any vendor-specific response representation.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record ShahkarVerification(boolean matched, String trackingId) {
}
