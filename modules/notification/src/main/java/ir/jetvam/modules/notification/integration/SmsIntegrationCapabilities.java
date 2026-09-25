package ir.jetvam.modules.notification.integration;

/**
 * Declares external integration capability codes owned by the notification module.
 * The integration core treats these codes as opaque routing keys.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class SmsIntegrationCapabilities {

    public static final String SMS_SEND = "SMS_SEND";

    private SmsIntegrationCapabilities() {
    }
}
