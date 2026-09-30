package ir.jetvam.apps.jobs.infrastructure.scheduler;

import ir.jetvam.apps.jobs.infrastructure.persistence.JobDefinitionEntity;
import ir.jetvam.apps.jobs.infrastructure.repository.JobDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobScheduleRegistryTest {

    @Test
    void registersEnabledDatabaseDefinitionsOnStartup() {
        UUID definitionId = UUID.randomUUID();
        JobDefinitionEntity definition = mock(JobDefinitionEntity.class);
        when(definition.getId()).thenReturn(definitionId);
        when(definition.getCode()).thenReturn("inquiry-dispatch");
        when(definition.getCronExpression()).thenReturn("0/15 * * * * ?");
        when(definition.getTimeZone()).thenReturn("Asia/Tehran");
        when(definition.isEnabled()).thenReturn(true);
        JobDefinitionRepository repository = mock(JobDefinitionRepository.class);
        when(repository.findAll()).thenReturn(List.of(definition));
        TaskScheduler scheduler = mock(TaskScheduler.class);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(scheduler).schedule(any(Runnable.class), any(Trigger.class));
        JobLauncher launcher = mock(JobLauncher.class);
        JobScheduleRegistry registry = new JobScheduleRegistry(repository, scheduler, launcher);

        registry.run(null);
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(task.capture(), any(Trigger.class));
        task.getValue().run();

        verify(launcher).launchScheduled(definitionId);
    }
}
