package ir.jetvam.apps.jobs.infrastructure.service;

import ir.jetvam.apps.jobs.infrastructure.handler.JobContext;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandler;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandlerRegistry;
import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobExecutionCoordinatorTest {

    @Test
    void storesSuccessfulHandlerResult() throws Exception {
        UUID definitionId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        JobContext context = context(definitionId, executionId);
        JobExecutionPersistenceService persistence = mock(JobExecutionPersistenceService.class);
        when(persistence.start(definitionId, null, JobTriggerType.SCHEDULED, null, context.scheduledAt()))
                .thenReturn(new JobExecutionPersistenceService.StartedExecution(context, "test-handler"));

        JobExecutionCoordinator coordinator = new JobExecutionCoordinator(
                persistence,
                new JobHandlerRegistry(List.of(handler(JobResult.completed("done"), null)))
        );
        coordinator.execute(definitionId, null, JobTriggerType.SCHEDULED, null, context.scheduledAt());

        verify(persistence).succeed(
                org.mockito.ArgumentMatchers.eq(executionId),
                argThat(result -> result.processedCount() == 0 && "done".equals(result.summary()))
        );
    }

    @Test
    void storesFailureAndRethrowsIt() {
        UUID definitionId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        JobContext context = context(definitionId, executionId);
        JobExecutionPersistenceService persistence = mock(JobExecutionPersistenceService.class);
        when(persistence.start(definitionId, null, JobTriggerType.SCHEDULED, null, context.scheduledAt()))
                .thenReturn(new JobExecutionPersistenceService.StartedExecution(context, "test-handler"));
        IllegalStateException failure = new IllegalStateException("failed");
        JobExecutionCoordinator coordinator = new JobExecutionCoordinator(
                persistence,
                new JobHandlerRegistry(List.of(handler(null, failure)))
        );

        Exception thrown = assertThrows(Exception.class, () -> coordinator.execute(
                definitionId, null, JobTriggerType.SCHEDULED, null, context.scheduledAt()
        ));

        assertSame(failure, thrown);
        verify(persistence).fail(
                org.mockito.ArgumentMatchers.eq(executionId),
                org.mockito.ArgumentMatchers.same(failure),
                org.mockito.ArgumentMatchers.contains("IllegalStateException")
        );
    }

    private static JobContext context(UUID definitionId, UUID executionId) {
        Instant scheduledAt = Instant.parse("2026-09-23T10:00:00Z");
        return new JobContext(
                executionId, definitionId, "test-job", JobTriggerType.SCHEDULED,
                null, scheduledAt, scheduledAt.plusSeconds(1)
        );
    }

    private static JobHandler handler(JobResult result, RuntimeException failure) {
        return new JobHandler() {
            @Override
            public String key() {
                return "test-handler";
            }

            @Override
            public JobResult execute(JobContext context) {
                if (failure != null) {
                    throw failure;
                }
                return result;
            }
        };
    }
}
