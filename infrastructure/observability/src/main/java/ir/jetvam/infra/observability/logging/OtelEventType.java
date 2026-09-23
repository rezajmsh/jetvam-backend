package ir.jetvam.infra.observability.logging;

/**
 * Enumerates structured event categories emitted by observability infrastructure.
 * Each value produces a stable event-type attribute.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public enum OtelEventType {
    HTTP_SERVER_REQUEST("http.server.request"),
    HTTP_CLIENT_REQUEST("http.client.request"),
    NOTIFICATION_DELIVERY("notification.delivery"),
    REPOSITORY_OPERATION("repository.operation"),
    AUDIT("audit"),
    APPLICATION("application");

    private final String value;

    OtelEventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
