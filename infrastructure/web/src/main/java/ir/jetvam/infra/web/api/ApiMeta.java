package ir.jetvam.infra.web.api;

import java.time.Instant;

public record ApiMeta(Instant timestamp, String requestId, String traceId) {
}
