package ir.jetvam.modules.integration;

/**
 * Declares stable capability codes used by provider routing and administration.
 * New external operations add codes without changing the generic routing infrastructure.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public final class IntegrationCapabilities {

    public static final String SHAHKAR_VERIFY = "SHAHKAR_VERIFY";
    public static final String SMS_SEND = "SMS_SEND";

    private IntegrationCapabilities() {
    }
}
