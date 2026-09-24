package ir.jetvam.apps.jobs.infrastructure.quartz;

import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;
import ir.jetvam.apps.jobs.infrastructure.service.JobExecutionCoordinator;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.Instant;
import java.util.UUID;

/**
 * Single Quartz adapter that dispatches every definition to its registered handler.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@DisallowConcurrentExecution
public class DelegatingQuartzJob extends QuartzJobBean {

    static final String DEFINITION_ID = "definitionId";
    static final String EXECUTION_ID = "executionId";
    static final String TRIGGER_TYPE = "triggerType";
    static final String REQUESTED_BY = "requestedBy";

    @Autowired
    private JobExecutionCoordinator coordinator;

    @Override
    protected void executeInternal(JobExecutionContext quartzContext) throws JobExecutionException {
        try {
            String executionId = quartzContext.getMergedJobDataMap().getString(EXECUTION_ID);
            coordinator.execute(
                    UUID.fromString(quartzContext.getMergedJobDataMap().getString(DEFINITION_ID)),
                    executionId == null ? null : UUID.fromString(executionId),
                    JobTriggerType.valueOf(quartzContext.getMergedJobDataMap().getString(TRIGGER_TYPE)),
                    quartzContext.getMergedJobDataMap().getString(REQUESTED_BY),
                    scheduledAt(quartzContext)
            );
        } catch (Exception exception) {
            throw new JobExecutionException(exception, false);
        }
    }

    private static Instant scheduledAt(JobExecutionContext context) {
        return context.getScheduledFireTime() == null
                ? null
                : context.getScheduledFireTime().toInstant();
    }
}
