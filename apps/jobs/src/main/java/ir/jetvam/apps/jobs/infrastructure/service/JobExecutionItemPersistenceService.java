package ir.jetvam.apps.jobs.infrastructure.service;

import ir.jetvam.apps.jobs.infrastructure.persistence.JobExecutionEntity;
import ir.jetvam.apps.jobs.infrastructure.persistence.JobExecutionItemEntity;
import ir.jetvam.apps.jobs.infrastructure.repository.JobExecutionItemRepository;
import ir.jetvam.apps.jobs.infrastructure.repository.JobExecutionRepository;
import ir.jetvam.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists handler-neutral item snapshots independently from the long-running job transaction.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
@RequiredArgsConstructor
public class JobExecutionItemPersistenceService {

    private final JobExecutionRepository executionRepository;
    private final JobExecutionItemRepository itemRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID executionId, List<JobExecutionItemCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        JobExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("job execution", executionId));
        List<JobExecutionItemEntity> items = new ArrayList<>(commands.size());
        for (int index = 0; index < commands.size(); index++) {
            JobExecutionItemCommand command = commands.get(index);
            items.add(new JobExecutionItemEntity(
                    execution, index + 1, command.itemType(), command.itemKey(), command.operationCode(),
                    command.subjectIdentifier(), command.subjectKey(), command.status(), command.businessStatus(),
                    command.providerCode(), command.externalReference(), command.message()
            ));
        }
        itemRepository.saveAll(items);
    }
}
