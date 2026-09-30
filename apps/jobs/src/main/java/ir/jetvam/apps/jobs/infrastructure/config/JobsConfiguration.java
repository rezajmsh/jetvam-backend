package ir.jetvam.apps.jobs.infrastructure.config;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandler;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandlerRegistry;
import ir.jetvam.apps.jobs.infrastructure.repository.JobDefinitionRepository;
import ir.jetvam.apps.jobs.infrastructure.repository.JobExecutionRepository;
import ir.jetvam.apps.jobs.infrastructure.scheduler.JobExecutionLockService;
import ir.jetvam.apps.jobs.infrastructure.scheduler.JobLauncher;
import ir.jetvam.apps.jobs.infrastructure.scheduler.JobScheduleRegistry;
import ir.jetvam.apps.jobs.infrastructure.service.DefaultJobManagementService;
import ir.jetvam.apps.jobs.infrastructure.service.JobExecutionCoordinator;
import ir.jetvam.apps.jobs.infrastructure.service.JobExecutionPersistenceService;
import ir.jetvam.apps.jobs.infrastructure.service.JobManagementService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;

/**
 * Auto-configures persistent job management while handlers remain application-owned.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "jetvam.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JobsConfiguration {

    @Bean
    JobHandlerRegistry jetvamJobHandlerRegistry(ObjectProvider<JobHandler> handlers) {
        return new JobHandlerRegistry(handlers.orderedStream().toList());
    }

    @Bean
    JobExecutionPersistenceService jetvamJobExecutionPersistenceService(
            JobDefinitionRepository definitions,
            JobExecutionRepository executions,
            TimeProvider timeProvider
    ) {
        return new JobExecutionPersistenceService(definitions, executions, timeProvider);
    }

    @Bean
    JobExecutionCoordinator jetvamJobExecutionCoordinator(
            JobExecutionPersistenceService persistenceService,
            JobHandlerRegistry handlerRegistry
    ) {
        return new JobExecutionCoordinator(persistenceService, handlerRegistry);
    }

    @Bean
    ThreadPoolTaskScheduler jetvamJobTaskScheduler(
            @Value("${jetvam.jobs.scheduler-thread-count:2}") int threadCount
    ) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(threadCount);
        scheduler.setThreadNamePrefix("jetvam-job-scheduler-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }

    SimpleAsyncTaskExecutor jetvamJobTaskExecutor(
            @Value("${jetvam.jobs.execution-concurrency:10}") int concurrency
    ) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("jetvam-job-execution-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(concurrency);
        executor.setTaskTerminationTimeout(30_000);
        return executor;
    }

    JobExecutionLockService jetvamJobExecutionLockService(
            JobDefinitionRepository definitions,
            TimeProvider timeProvider,
            @Value("${jetvam.jobs.instance-id:${spring.application.name}-${random.uuid}}") String instanceId,
            @Value("${jetvam.jobs.lock-duration:1h}") Duration lockDuration
    ) {
        return new JobExecutionLockService(definitions, timeProvider, instanceId, lockDuration);
    }

    JobLauncher jetvamJobLauncher(
            SimpleAsyncTaskExecutor jetvamJobTaskExecutor,
            JobExecutionCoordinator coordinator,
            JobExecutionLockService lockService,
            TimeProvider timeProvider
    ) {
        return new JobLauncher(jetvamJobTaskExecutor, coordinator, lockService, timeProvider);
    }

    JobScheduleRegistry jetvamJobScheduleRegistry(
            JobDefinitionRepository definitions,
            ThreadPoolTaskScheduler jetvamJobTaskScheduler,
            JobLauncher launcher
    ) {
        return new JobScheduleRegistry(definitions, jetvamJobTaskScheduler, launcher);
    }

    @Bean
    JobManagementService jetvamJobManagementService(
            JobDefinitionRepository definitions,
            JobExecutionRepository executions,
            JobExecutionPersistenceService persistenceService,
            JobExecutionCoordinator coordinator,
            JobHandlerRegistry handlerRegistry,
            JobScheduleRegistry scheduleRegistry,
            JobLauncher launcher
    ) {
        return new DefaultJobManagementService(
                definitions, executions, persistenceService, coordinator, handlerRegistry, scheduleRegistry, launcher
        );
    }
}
