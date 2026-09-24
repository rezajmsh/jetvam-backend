package ir.jetvam.modules.integration.shahkar;

/**
 * Carries the canonical identifiers required for a Shahkar ownership inquiry.
 * Provider adapters translate this command to their own wire contracts and signatures.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record ShahkarCommand(String mobile, String nationalCode) {
}
