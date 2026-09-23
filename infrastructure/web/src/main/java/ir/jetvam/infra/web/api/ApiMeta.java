package ir.jetvam.infra.web.api;

import java.time.Instant;

/**
 * Carries response timestamp and request correlation identifiers.
 * Metadata lets clients connect API results with distributed traces.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public record ApiMeta(Instant timestamp, String requestId, String traceId) {
}
