package ir.jetvam.apps.jobs.infrastructure.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Administrative use cases for definitions, history, and manual execution. */
public interface JobManagementService {

    JobDefinitionView create(CreateJobDefinitionCommand command);

    JobDefinitionView update(UUID id, UpdateJobDefinitionCommand command);

    JobDefinitionView setEnabled(UUID id, boolean enabled);

    JobDefinitionView get(UUID id);

    List<JobDefinitionView> findAll();

    Page<JobExecutionView> history(UUID definitionId, Pageable pageable);

    JobStatisticsView statistics(UUID definitionId);

    JobExecutionView executeManually(UUID definitionId, String requestedBy);

    List<String> registeredHandlerKeys();
}
