package ir.jetvam.modules.notification.delivery;

/** Item-level outcome of one notification outbox batch. */
public record NotificationBatchResult(int processedCount, int succeededCount, int failedCount) {

    public NotificationBatchResult {
        if (processedCount < 0 || succeededCount < 0 || failedCount < 0) {
            throw new IllegalArgumentException("Notification batch counters must not be negative");
        }
        if (processedCount != succeededCount + failedCount) {
            throw new IllegalArgumentException("processedCount must equal succeededCount + failedCount");
        }
    }
}
