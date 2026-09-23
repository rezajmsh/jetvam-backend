package ir.jetvam.modules.notification.security;

import java.util.Map;

/**
 * Contains sensitive delivery data that is encrypted before persistence.
 * Destination and template parameters exist in plaintext only in process memory.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record NotificationPayload(String destination, Map<String, String> parameters) {
}
