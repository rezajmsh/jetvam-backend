package ir.jetvam.apps.jobs.infrastructure.scheduler;

import ir.jetvam.apps.jobs.infrastructure.repository.JobDefinitionRepository;
import ir.jetvam.common.time.ClockTimeProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobExecutionLockServiceTest {

    @Test
    void acquiresAndReleasesADatabaseLease() {
        Instant now = Instant.parse("2026-09-29T10:00:00Z");
        UUID definitionId = UUID.randomUUID();
        JobDefinitionRepository repository = mock(JobDefinitionRepository.class);
        when(repository.tryAcquireExecutionLock(
                org.mockito.ArgumentMatchers.eq(definitionId), anyString(),
                org.mockito.ArgumentMatchers.eq(now), org.mockito.ArgumentMatchers.eq(now.plusSeconds(300))
        )).thenReturn(1);
        JobExecutionLockService service = new JobExecutionLockService(
                repository, new ClockTimeProvider(Clock.fixed(now, ZoneOffset.UTC)), "jobs-1", Duration.ofMinutes(5)
        );

        String owner = service.tryAcquire(definitionId);
        service.release(definitionId, owner);

        assertThat(owner).startsWith("jobs-1:");
        verify(repository).releaseExecutionLock(definitionId, owner);
    }
}
