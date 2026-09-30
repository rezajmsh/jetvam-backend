package ir.jetvam.apps.jobs.infrastructure.scheduler;

import ir.jetvam.apps.jobs.infrastructure.persistence.JobDefinitionEntity;
import ir.jetvam.apps.jobs.infrastructure.repository.JobDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Registers database-owned cron definitions on startup and refreshes registrations after configuration changes.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@RequiredArgsConstructor
public class JobScheduleRegistry implements ApplicationRunner {

    private final JobDefinitionRepository repository;
    private final TaskScheduler taskScheduler;
    private final JobLauncher launcher;
    private final Map<UUID, ScheduledFuture<?>> schedules = new ConcurrentHashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        repository.findAll().forEach(this::synchronize);
    }

    public void synchronizeAfterCommit(UUID definitionId) {
        Runnable synchronization = () -> repository.findById(definitionId).ifPresent(this::synchronize);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    synchronization.run();
                }
            });
        } else {
            synchronization.run();
        }
    }

    private synchronized void synchronize(JobDefinitionEntity definition) {
        ScheduledFuture<?> existing = schedules.remove(definition.getId());
        if (existing != null) {
            existing.cancel(false);
        }
        if (!definition.isEnabled()) {
            return;
        }
        CronTrigger trigger = new CronTrigger(
                definition.getCronExpression(), ZoneId.of(definition.getTimeZone())
        );
        ScheduledFuture<?> scheduled = taskScheduler.schedule(
                () -> launcher.launchScheduled(definition.getId()), trigger
        );
        if (scheduled == null) {
            throw new IllegalStateException("Could not register job schedule: " + definition.getCode());
        }
        schedules.put(definition.getId(), scheduled);
    }
}
