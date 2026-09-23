package ir.jetvam.apps.jobs.notification;

import ir.jetvam.apps.jobs.infrastructure.handler.JobContext;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandler;
import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.modules.notification.delivery.NotificationBatchResult;
import ir.jetvam.modules.notification.delivery.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Jobs-app adapter; notification delivery remains owned by the notification module. */
@Component
@RequiredArgsConstructor
public class NotificationDispatchJobHandler implements JobHandler {

    public static final String KEY = "notification-dispatch";

    private final NotificationDeliveryService deliveryService;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public JobResult execute(JobContext context) {
        NotificationBatchResult result = deliveryService.dispatchBatch();
        return JobResult.completed(
                result.processedCount(),
                result.succeededCount(),
                result.failedCount(),
                "Notification outbox batch completed"
        );
    }
}
