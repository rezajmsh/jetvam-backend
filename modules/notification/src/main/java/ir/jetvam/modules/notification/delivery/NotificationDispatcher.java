package ir.jetvam.modules.notification.delivery;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the local transactional outbox when dispatch is enabled for an application.
 * Fixed-delay execution avoids overlapping polls within the same process.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jetvam.notification.dispatcher", name = "enabled", havingValue = "true")
public class NotificationDispatcher {

    private final NotificationDeliveryService deliveryService;

    @Scheduled(fixedDelayString = "${jetvam.notification.dispatcher.fixed-delay:1s}")
    public void dispatch() {
        deliveryService.dispatchBatch();
    }
}
