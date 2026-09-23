package ir.jetvam.apps.jobs.infrastructure.handler;

/** Item-level counters and an optional non-sensitive execution summary. */
public record JobResult(long processedCount, long succeededCount, long failedCount, String summary) {

    public JobResult {
        if (processedCount < 0 || succeededCount < 0 || failedCount < 0) {
            throw new IllegalArgumentException("Job result counters must not be negative");
        }
        if (processedCount != succeededCount + failedCount) {
            throw new IllegalArgumentException("processedCount must equal succeededCount + failedCount");
        }
    }

    public static JobResult completed() {
        return new JobResult(0, 0, 0, null);
    }

    public static JobResult completed(String summary) {
        return new JobResult(0, 0, 0, summary);
    }

    public static JobResult completed(long processedCount, long succeededCount, long failedCount, String summary) {
        return new JobResult(processedCount, succeededCount, failedCount, summary);
    }
}
