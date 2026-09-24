package ir.jetvam.apps.jobs.infrastructure.quartz;

import ir.jetvam.apps.jobs.infrastructure.repository.JobDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Restores Quartz state from managed definitions whenever the application starts.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@RequiredArgsConstructor
public class JobScheduleReconciler implements ApplicationRunner {

    private final JobDefinitionRepository repository;
    private final JobScheduleSynchronizer synchronizer;

    @Override
    public void run(ApplicationArguments args) {
        repository.findAll().forEach(synchronizer::synchronize);
    }
}
