package ir.jetvam.modules.notification.observability;

import io.micrometer.core.instrument.Tag;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.logging.OtelEventType;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.modules.notification.config.NotificationProperties;
import ir.jetvam.modules.notification.delivery.ClaimedNotification;
import ir.jetvam.modules.notification.delivery.NotificationTransition;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

import java.util.List;
import java.time.Duration;

/**
 * Emits safe delivery lifecycle logs and bounded notification metrics.
 * Destinations, parameters and provider response bodies are deliberately excluded.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Component
@RequiredArgsConstructor
public class NotificationTelemetry {

    private static final Logger LOGGER = LoggerFactory.getLogger("jetvam.notification.delivery");

    private final OtelEventLogger eventLogger;
    private final InfrastructureMetrics metrics;
    private final JetvamObservabilityProperties observabilityProperties;
    private final NotificationProperties notificationProperties;

    public void sent(ClaimedNotification notification, int attemptCount, Duration duration) {
        record(notification, "sent", attemptCount, null, duration, Level.INFO);
    }

    public void failed(ClaimedNotification notification, NotificationTransition transition, Duration duration) {
        String outcome = transition.status().name().toLowerCase();
        record(notification, outcome, transition.attemptCount(), transition.errorCode(), duration,
                transition.status() == ir.jetvam.modules.notification.model.NotificationStatus.DEAD
                        ? Level.ERROR : Level.WARN);
    }

    private void record(
            ClaimedNotification notification,
            String outcome,
            int attemptCount,
            String errorCode,
            Duration duration,
            Level level
    ) {
        NotificationProperties.Observability settings = notificationProperties.getObservability();
        if (!observabilityProperties.isEnabled() || !settings.isEnabled()) {
            return;
        }
        if (observabilityProperties.getLogging().isEnabled() && settings.isLoggingEnabled()) {
            eventLogger.log(
                    LOGGER,
                    level,
                    "notification.delivery.completed",
                    OtelEventType.NOTIFICATION_DELIVERY,
                    "Notification delivery attempt completed",
                    eventLogger.attributes(
                            "notification.id", notification.id(),
                            "notification.channel", notification.channel().name(),
                            "notification.template", notification.templateCode(),
                            "notification.attempt", attemptCount,
                            "operation.outcome", outcome,
                            "error.code", errorCode,
                            "duration_ms", duration.toNanos() / 1_000_000.0
                    )
            );
        }
        if (observabilityProperties.getMetrics().isEnabled() && settings.isMetricsEnabled()) {
            List<Tag> tags = List.of(
                    Tag.of("channel", notification.channel().name()),
                    Tag.of("outcome", outcome)
            );
            metrics.increment("notification.delivery.attempts", tags);
            metrics.record("notification.delivery.duration", duration, tags);
        }
    }
}
