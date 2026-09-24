package ir.jetvam.apps.jobs.infrastructure.service;

import ir.jetvam.apps.jobs.infrastructure.handler.JobHandler;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandlerRegistry;
import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;
import lombok.RequiredArgsConstructor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.UUID;

/**
 * Executes a resolved handler while guaranteeing terminal history updates.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@RequiredArgsConstructor
public class JobExecutionCoordinator {

    private final JobExecutionPersistenceService persistenceService;
    private final JobHandlerRegistry handlerRegistry;

    public void execute(
            UUID definitionId,
            UUID executionId,
            JobTriggerType triggerType,
            String requestedBy,
            Instant scheduledAt
    ) throws Exception {
        JobExecutionPersistenceService.StartedExecution started = persistenceService.start(
                definitionId,
                executionId,
                triggerType,
                requestedBy,
                scheduledAt
        );
        try {
            JobHandler handler = handlerRegistry.require(started.handlerKey());
            JobResult result = handler.execute(started.context());
            persistenceService.succeed(started.context().executionId(), result == null ? JobResult.completed() : result);
        } catch (Exception exception) {
            persistenceService.fail(started.context().executionId(), exception, stackTrace(exception));
            throw exception;
        }
    }

    public void failQueued(UUID executionId, Throwable error) {
        persistenceService.fail(executionId, error, stackTrace(error));
    }

    private static String stackTrace(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
