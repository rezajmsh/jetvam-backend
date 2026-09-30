package ir.jetvam.apps.jobs.infrastructure.scheduler;

import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;
import ir.jetvam.apps.jobs.infrastructure.service.JobExecutionCoordinator;
import ir.jetvam.common.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;

import java.util.UUID;

/**
 * Launches scheduled and manual jobs outside HTTP and scheduler threads while preserving execution history.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@RequiredArgsConstructor
public class JobLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncher.class);

    private final TaskExecutor taskExecutor;
    private final JobExecutionCoordinator coordinator;
    private final JobExecutionLockService lockService;
    private final TimeProvider timeProvider;

    public void launchScheduled(UUID definitionId) {
        taskExecutor.execute(() -> execute(definitionId, null, JobTriggerType.SCHEDULED, null));
    }

    public void launchManual(UUID definitionId, UUID executionId, String requestedBy) {
        taskExecutor.execute(() -> execute(definitionId, executionId, JobTriggerType.MANUAL, requestedBy));
    }

    private void execute(
            UUID definitionId,
            UUID executionId,
            JobTriggerType triggerType,
            String requestedBy
    ) {
        String owner = lockService.tryAcquire(definitionId);
        if (owner == null) {
            if (executionId != null) {
                coordinator.failQueued(executionId, new IllegalStateException("Job is already running"));
            }
            LOGGER.info("Job execution skipped because another instance owns the lease: definitionId={}", definitionId);
            return;
        }
        try {
            coordinator.execute(
                    definitionId, executionId, triggerType, requestedBy, timeProvider.now()
            );
        } catch (Exception exception) {
            LOGGER.error("Job execution failed: definitionId={}, triggerType={}",
                    definitionId, triggerType, exception);
        } finally {
            lockService.release(definitionId, owner);
        }
    }
}
