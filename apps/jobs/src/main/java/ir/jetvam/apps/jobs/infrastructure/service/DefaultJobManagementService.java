package ir.jetvam.apps.jobs.infrastructure.service;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandlerRegistry;
import ir.jetvam.apps.jobs.infrastructure.persistence.JobDefinitionEntity;
import ir.jetvam.apps.jobs.infrastructure.persistence.JobExecutionEntity;
import ir.jetvam.apps.jobs.infrastructure.quartz.JobScheduleSynchronizer;
import ir.jetvam.apps.jobs.infrastructure.repository.JobDefinitionRepository;
import ir.jetvam.apps.jobs.infrastructure.repository.JobExecutionRepository;
import ir.jetvam.apps.jobs.infrastructure.repository.JobExecutionStatistics;
import lombok.RequiredArgsConstructor;
import org.quartz.CronExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class DefaultJobManagementService implements JobManagementService {

    private final JobDefinitionRepository definitionRepository;
    private final JobExecutionRepository executionRepository;
    private final JobExecutionPersistenceService executionPersistenceService;
    private final JobExecutionCoordinator executionCoordinator;
    private final JobHandlerRegistry handlerRegistry;
    private final JobScheduleSynchronizer scheduleSynchronizer;

    @Override
    @Transactional
    public JobDefinitionView create(CreateJobDefinitionCommand command) {
        Preconditions.requireNonNull(command, "command");
        String code = required(command.code(), "code");
        if (definitionRepository.existsByCode(code)) {
            throw new ConflictException("Job code already exists: " + code);
        }
        ValidatedDefinition values = validate(
                command.displayName(),
                command.description(),
                command.handlerKey(),
                command.cronExpression(),
                command.timeZone()
        );
        JobDefinitionEntity definition = definitionRepository.saveAndFlush(new JobDefinitionEntity(
                code,
                values.displayName(),
                values.description(),
                values.handlerKey(),
                values.cronExpression(),
                values.timeZone(),
                command.enabled()
        ));
        scheduleSynchronizer.synchronize(definition);
        return toView(definition);
    }

    @Override
    @Transactional
    public JobDefinitionView update(UUID id, UpdateJobDefinitionCommand command) {
        Preconditions.requireNonNull(command, "command");
        JobDefinitionEntity definition = requireDefinition(id);
        ValidatedDefinition values = validate(
                command.displayName(),
                command.description(),
                command.handlerKey(),
                command.cronExpression(),
                command.timeZone()
        );
        definition.update(
                definition.getCode(),
                values.displayName(),
                values.description(),
                values.handlerKey(),
                values.cronExpression(),
                values.timeZone(),
                command.enabled()
        );
        definitionRepository.flush();
        scheduleSynchronizer.synchronize(definition);
        return toView(definition);
    }

    @Override
    @Transactional
    public JobDefinitionView setEnabled(UUID id, boolean enabled) {
        JobDefinitionEntity definition = requireDefinition(id);
        definition.setEnabled(enabled);
        definitionRepository.flush();
        scheduleSynchronizer.synchronize(definition);
        return toView(definition);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDefinitionView get(UUID id) {
        return toView(requireDefinition(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobDefinitionView> findAll() {
        return definitionRepository.findAll().stream()
                .sorted((left, right) -> left.getCode().compareTo(right.getCode()))
                .map(DefaultJobManagementService::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobExecutionView> history(UUID definitionId, Pageable pageable) {
        if (!definitionRepository.existsById(definitionId)) {
            throw new ResourceNotFoundException("job definition", definitionId);
        }
        return executionRepository.findAllByDefinition_Id(definitionId, pageable).map(DefaultJobManagementService::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public JobStatisticsView statistics(UUID definitionId) {
        if (!definitionRepository.existsById(definitionId)) {
            throw new ResourceNotFoundException("job definition", definitionId);
        }
        JobExecutionStatistics statistics = executionRepository.statistics(definitionId);
        return new JobStatisticsView(
                definitionId,
                statistics.getExecutionCount(),
                statistics.getSucceededExecutionCount(),
                statistics.getFailedExecutionCount(),
                statistics.getProcessedCount(),
                statistics.getSucceededCount(),
                statistics.getFailedCount()
        );
    }

    @Override
    public JobExecutionView executeManually(UUID definitionId, String requestedBy) {
        JobDefinitionEntity definition = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new ResourceNotFoundException("job definition", definitionId));
        handlerRegistry.require(definition.getHandlerKey());
        JobExecutionEntity execution = executionPersistenceService.queueManual(definitionId, requestedBy);
        try {
            scheduleSynchronizer.triggerManually(definition, execution.getId(), requestedBy);
        } catch (RuntimeException exception) {
            executionCoordinator.failQueued(execution.getId(), exception);
            throw exception;
        }
        return toView(execution);
    }

    @Override
    public List<String> registeredHandlerKeys() {
        return handlerRegistry.keys();
    }

    private ValidatedDefinition validate(
            String displayName,
            String description,
            String handlerKey,
            String cronExpression,
            String timeZone
    ) {
        String normalizedHandler = required(handlerKey, "handlerKey");
        handlerRegistry.require(normalizedHandler);
        String normalizedCron = required(cronExpression, "cronExpression");
        Preconditions.require(CronExpression.isValidExpression(normalizedCron), "cronExpression is invalid");
        String normalizedZone = required(timeZone, "timeZone");
        ZoneId.of(normalizedZone);
        return new ValidatedDefinition(
                required(displayName, "displayName"),
                description == null || description.isBlank() ? null : description.strip(),
                normalizedHandler,
                normalizedCron,
                normalizedZone
        );
    }

    private JobDefinitionEntity requireDefinition(UUID id) {
        Preconditions.requireNonNull(id, "id");
        return definitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("job definition", id));
    }

    private static String required(String value, String name) {
        return Preconditions.requireText(value, name).strip();
    }

    private static JobDefinitionView toView(JobDefinitionEntity definition) {
        return new JobDefinitionView(
                definition.getId(), definition.getCode(), definition.getDisplayName(), definition.getDescription(),
                definition.getHandlerKey(), definition.getCronExpression(), definition.getTimeZone(),
                definition.isEnabled(), definition.getVersion(), definition.getCreatedAt(), definition.getUpdatedAt()
        );
    }

    private static JobExecutionView toView(JobExecutionEntity execution) {
        return new JobExecutionView(
                execution.getId(), execution.getDefinition().getId(), execution.getJobCode(), execution.getHandlerKey(),
                execution.getTriggerType(), execution.getStatus(), execution.getRequestedBy(), execution.getScheduledAt(),
                execution.getStartedAt(), execution.getFinishedAt(), execution.getDurationMs(),
                execution.getProcessedCount(), execution.getSucceededCount(), execution.getFailedCount(),
                execution.getResultSummary(),
                execution.getErrorType(), execution.getErrorMessage()
        );
    }

    private record ValidatedDefinition(
            String displayName,
            String description,
            String handlerKey,
            String cronExpression,
            String timeZone
    ) {
    }
}
