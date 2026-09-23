package ir.jetvam.apps.jobs.notification;

import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.modules.notification.delivery.NotificationDeliveryService;
import ir.jetvam.modules.notification.delivery.NotificationBatchResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatchJobHandlerTest {

    @Test
    void delegatesDeliveryToNotificationModule() {
        NotificationDeliveryService deliveryService = mock(NotificationDeliveryService.class);
        when(deliveryService.dispatchBatch()).thenReturn(new NotificationBatchResult(3, 2, 1));
        NotificationDispatchJobHandler handler = new NotificationDispatchJobHandler(deliveryService);

        JobResult result = handler.execute(null);

        assertEquals("notification-dispatch", handler.key());
        assertEquals(3, result.processedCount());
        assertEquals(2, result.succeededCount());
        assertEquals(1, result.failedCount());
        assertEquals("Notification outbox batch completed", result.summary());
        verify(deliveryService).dispatchBatch();
    }
}
