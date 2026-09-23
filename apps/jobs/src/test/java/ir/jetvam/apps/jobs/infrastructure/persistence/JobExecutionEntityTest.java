package ir.jetvam.apps.jobs.infrastructure.persistence;

import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.apps.jobs.infrastructure.model.JobExecutionStatus;
import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobExecutionEntityTest {

    @Test
    void storesItemLevelCountersFromHandlerResult() {
        JobDefinitionEntity definition = new JobDefinitionEntity(
                "notification-dispatch", "Notification dispatch", null,
                "notification-dispatch", "0/5 * * * * ?", "Asia/Tehran", true
        );
        Instant startedAt = Instant.parse("2026-09-23T10:00:00Z");
        JobExecutionEntity execution = new JobExecutionEntity(
                definition, JobTriggerType.SCHEDULED, JobExecutionStatus.RUNNING, null, startedAt
        );
        execution.markRunning(startedAt);

        execution.markSucceeded(
                startedAt.plusSeconds(2),
                JobResult.completed(5, 3, 2, "completed")
        );

        assertEquals(JobExecutionStatus.SUCCEEDED, execution.getStatus());
        assertEquals(5, execution.getProcessedCount());
        assertEquals(3, execution.getSucceededCount());
        assertEquals(2, execution.getFailedCount());
        assertEquals(2000, execution.getDurationMs());
    }
}
