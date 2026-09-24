package ir.jetvam.apps.jobs.infrastructure.config;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandler;
import ir.jetvam.apps.jobs.infrastructure.handler.JobHandlerRegistry;
import ir.jetvam.apps.jobs.infrastructure.quartz.AutowiringSpringBeanJobFactory;
import ir.jetvam.apps.jobs.infrastructure.quartz.JobScheduleReconciler;
import ir.jetvam.apps.jobs.infrastructure.quartz.JobScheduleSynchronizer;
import ir.jetvam.apps.jobs.infrastructure.repository.JobDefinitionRepository;
import ir.jetvam.apps.jobs.infrastructure.repository.JobExecutionRepository;
import ir.jetvam.apps.jobs.infrastructure.service.DefaultJobManagementService;
import ir.jetvam.apps.jobs.infrastructure.service.JobExecutionCoordinator;
import ir.jetvam.apps.jobs.infrastructure.service.JobExecutionPersistenceService;
import ir.jetvam.apps.jobs.infrastructure.service.JobManagementService;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configures persistent job management while handlers remain application-owned.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Scheduler.class)
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
    JobScheduleSynchronizer jetvamJobScheduleSynchronizer(Scheduler scheduler) {
        return new JobScheduleSynchronizer(scheduler);
    }

    @Bean
    JobManagementService jetvamJobManagementService(
            JobDefinitionRepository definitions,
            JobExecutionRepository executions,
            JobExecutionPersistenceService persistenceService,
            JobExecutionCoordinator coordinator,
            JobHandlerRegistry handlerRegistry,
            JobScheduleSynchronizer synchronizer
    ) {
        return new DefaultJobManagementService(
                definitions, executions, persistenceService, coordinator, handlerRegistry, synchronizer
        );
    }

    @Bean
    JobScheduleReconciler jetvamJobScheduleReconciler(
            JobDefinitionRepository repository,
            JobScheduleSynchronizer synchronizer
    ) {
        return new JobScheduleReconciler(repository, synchronizer);
    }

    @Bean
    SchedulerFactoryBeanCustomizer jetvamQuartzJobFactoryCustomizer(ApplicationContext applicationContext) {
        return schedulerFactoryBean -> {
            AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
            jobFactory.setApplicationContext(applicationContext);
            schedulerFactoryBean.setJobFactory(jobFactory);
        };
    }
}
