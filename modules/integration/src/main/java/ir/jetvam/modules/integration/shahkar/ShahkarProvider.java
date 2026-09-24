package ir.jetvam.modules.integration.shahkar;

/**
 * Defines the consumer-independent capability for mobile ownership verification.
 * Provider transport, routing, authentication and TLS remain internal to Integration.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public interface ShahkarProvider {

    ShahkarVerification verify(String mobile, String nationalCode);
}
