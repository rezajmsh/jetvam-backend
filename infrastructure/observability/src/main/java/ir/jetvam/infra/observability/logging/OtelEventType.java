package ir.jetvam.infra.observability.logging;

public enum OtelEventType {
    HTTP_SERVER_REQUEST("http.server.request"),
    HTTP_CLIENT_REQUEST("http.client.request"),
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
