package ir.jetvam.apps.jobs.infrastructure.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.apps.jobs.infrastructure.handler.JobContext;
import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.apps.jobs.infrastructure.model.JobExecutionStatus;
import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;
import ir.jetvam.apps.jobs.infrastructure.persistence.JobDefinitionEntity;
import ir.jetvam.apps.jobs.infrastructure.persistence.JobExecutionEntity;
import ir.jetvam.apps.jobs.infrastructure.repository.JobDefinitionRepository;
import ir.jetvam.apps.jobs.infrastructure.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Keeps short history transactions separate from potentially long handler calls. */
@RequiredArgsConstructor
public class JobExecutionPersistenceService {

    private final JobDefinitionRepository definitionRepository;
    private final JobExecutionRepository executionRepository;
    private final TimeProvider timeProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobExecutionEntity queueManual(UUID definitionId, String requestedBy) {
        JobDefinitionEntity definition = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new ResourceNotFoundException("job definition", definitionId));
        return executionRepository.saveAndFlush(new JobExecutionEntity(
                definition,
                JobTriggerType.MANUAL,
                JobExecutionStatus.QUEUED,
                normalizeRequestedBy(requestedBy),
                timeProvider.now()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StartedExecution start(
            UUID definitionId,
            UUID executionId,
            JobTriggerType triggerType,
            String requestedBy,
            Instant scheduledAt
    ) {
        Instant now = timeProvider.now();
        JobExecutionEntity execution;
        if (executionId == null) {
            JobDefinitionEntity definition = definitionRepository.findById(definitionId)
                    .orElseThrow(() -> new ResourceNotFoundException("job definition", definitionId));
            execution = new JobExecutionEntity(
                    definition,
                    triggerType,
                    JobExecutionStatus.RUNNING,
                    normalizeRequestedBy(requestedBy),
                    scheduledAt
            );
            execution.markRunning(now);
            execution = executionRepository.saveAndFlush(execution);
        } else {
            execution = executionRepository.findByIdForUpdate(executionId)
                    .orElseThrow(() -> new ResourceNotFoundException("job execution", executionId));
            if (execution.getStatus() != JobExecutionStatus.QUEUED) {
                throw new IllegalStateException("Job execution is not queued: " + executionId);
            }
            execution.markRunning(now);
            executionRepository.flush();
        }
        JobContext context = new JobContext(
                execution.getId(),
                execution.getDefinition().getId(),
                execution.getJobCode(),
                execution.getTriggerType(),
                execution.getRequestedBy(),
                execution.getScheduledAt(),
                execution.getStartedAt()
        );
        return new StartedExecution(context, execution.getHandlerKey());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeed(UUID executionId, JobResult result) {
        JobExecutionEntity execution = executionRepository.findByIdForUpdate(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("job execution", executionId));
        execution.markSucceeded(timeProvider.now(), result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID executionId, Throwable error, String stackTrace) {
        JobExecutionEntity execution = executionRepository.findByIdForUpdate(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("job execution", executionId));
        execution.markFailed(timeProvider.now(), error, stackTrace);
    }

    private static String normalizeRequestedBy(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public record StartedExecution(JobContext context, String handlerKey) {
    }
}
