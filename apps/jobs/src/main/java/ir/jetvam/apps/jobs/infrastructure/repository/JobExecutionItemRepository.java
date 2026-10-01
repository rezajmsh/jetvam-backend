package ir.jetvam.apps.jobs.infrastructure.repository;

import ir.jetvam.apps.jobs.infrastructure.persistence.JobExecutionItemEntity;
import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Provides paged access to item-level job execution history.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public interface JobExecutionItemRepository extends JetvamJpaRepository<JobExecutionItemEntity, UUID> {

    Page<JobExecutionItemEntity> findAllByExecution_Id(UUID executionId, Pageable pageable);
}
