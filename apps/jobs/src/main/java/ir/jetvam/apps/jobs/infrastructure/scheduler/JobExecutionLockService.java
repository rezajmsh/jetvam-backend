package ir.jetvam.apps.jobs.infrastructure.scheduler;

import ir.jetvam.apps.jobs.infrastructure.repository.JobDefinitionRepository;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.Preconditions;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Acquires a database-backed lease so one logical job cannot run concurrently across application instances.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@RequiredArgsConstructor
public class JobExecutionLockService {

    private final JobDefinitionRepository repository;
    private final TimeProvider timeProvider;
    private final String instanceId;
    private final Duration lockDuration;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String tryAcquire(UUID definitionId) {
        Preconditions.requireNonNull(definitionId, "definitionId");
        Instant now = timeProvider.now();
        String owner = instanceId + ":" + UUID.randomUUID();
        int acquired = repository.tryAcquireExecutionLock(
                definitionId, owner, now, now.plus(lockDuration)
        );
        return acquired == 1 ? owner : null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(UUID definitionId, String owner) {
        if (owner != null) {
            repository.releaseExecutionLock(definitionId, owner);
        }
    }
}
