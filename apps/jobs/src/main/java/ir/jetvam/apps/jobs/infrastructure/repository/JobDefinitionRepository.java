package ir.jetvam.apps.jobs.infrastructure.repository;

import ir.jetvam.apps.jobs.infrastructure.persistence.JobDefinitionEntity;
import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobDefinitionRepository extends JetvamJpaRepository<JobDefinitionEntity, UUID> {

    Optional<JobDefinitionEntity> findByCode(String code);

    boolean existsByCode(String code);
}
