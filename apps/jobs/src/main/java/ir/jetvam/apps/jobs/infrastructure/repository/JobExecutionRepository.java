package ir.jetvam.apps.jobs.infrastructure.repository;

import ir.jetvam.apps.jobs.infrastructure.persistence.JobExecutionEntity;
import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Persists execution history and aggregates item-level counters for managed jobs.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public interface JobExecutionRepository extends JetvamJpaRepository<JobExecutionEntity, UUID> {

    Page<JobExecutionEntity> findAllByDefinition_Id(UUID definitionId, Pageable pageable);

    @Query(value = """
            select count(*) as "executionCount",
                   count(*) filter (where status = 'SUCCEEDED') as "succeededExecutionCount",
                   count(*) filter (where status = 'FAILED') as "failedExecutionCount",
                   coalesce(sum(processed_count), 0) as "processedCount",
                   coalesce(sum(succeeded_count), 0) as "succeededCount",
                   coalesce(sum(failed_count), 0) as "failedCount"
              from job_execution
             where job_definition_id = :definitionId
            """, nativeQuery = true)
    JobExecutionStatistics statistics(@Param("definitionId") UUID definitionId);
}
