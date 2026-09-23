package ir.jetvam.apps.jobs.infrastructure.quartz;

import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;
import ir.jetvam.apps.jobs.infrastructure.persistence.JobDefinitionEntity;
import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import java.util.TimeZone;
import java.util.UUID;

/** Reconciles application-owned definitions with durable Quartz jobs and triggers. */
@RequiredArgsConstructor
public class JobScheduleSynchronizer {

    public static final String JOB_GROUP = "jetvam-jobs";
    public static final String TRIGGER_GROUP = "jetvam-job-triggers";

    private final Scheduler scheduler;

    public void synchronize(JobDefinitionEntity definition) {
        try {
            JobDetail detail = jobDetail(definition);
            scheduler.addJob(detail, true, true);

            TriggerKey triggerKey = triggerKey(definition.getCode());
            if (!definition.isEnabled()) {
                if (scheduler.checkExists(triggerKey)) {
                    scheduler.unscheduleJob(triggerKey);
                }
                return;
            }

            Trigger trigger = cronTrigger(definition, detail.getKey());
            if (scheduler.checkExists(triggerKey)) {
                scheduler.rescheduleJob(triggerKey, trigger);
            } else {
                scheduler.scheduleJob(trigger);
            }
        } catch (SchedulerException exception) {
            throw new IllegalStateException("Could not synchronize job " + definition.getCode(), exception);
        }
    }

    public void triggerManually(JobDefinitionEntity definition, UUID executionId, String requestedBy) {
        synchronizeJobDetail(definition);
        JobDataMap data = new JobDataMap();
        data.put(DelegatingQuartzJob.EXECUTION_ID, executionId.toString());
        data.put(DelegatingQuartzJob.TRIGGER_TYPE, JobTriggerType.MANUAL.name());
        if (requestedBy != null && !requestedBy.isBlank()) {
            data.put(DelegatingQuartzJob.REQUESTED_BY, requestedBy.strip());
        }
        try {
            scheduler.triggerJob(jobKey(definition.getCode()), data);
        } catch (SchedulerException exception) {
            throw new IllegalStateException("Could not trigger job " + definition.getCode(), exception);
        }
    }

    private void synchronizeJobDetail(JobDefinitionEntity definition) {
        try {
            scheduler.addJob(jobDetail(definition), true, true);
        } catch (SchedulerException exception) {
            throw new IllegalStateException("Could not register job " + definition.getCode(), exception);
        }
    }

    private static JobDetail jobDetail(JobDefinitionEntity definition) {
        JobDataMap data = new JobDataMap();
        data.put(DelegatingQuartzJob.DEFINITION_ID, definition.getId().toString());
        data.put(DelegatingQuartzJob.TRIGGER_TYPE, JobTriggerType.SCHEDULED.name());
        return JobBuilder.newJob(DelegatingQuartzJob.class)
                .withIdentity(jobKey(definition.getCode()))
                .withDescription(definition.getDisplayName())
                .usingJobData(data)
                .storeDurably(true)
                .requestRecovery(true)
                .build();
    }

    private static Trigger cronTrigger(JobDefinitionEntity definition, JobKey jobKey) {
        CronScheduleBuilder schedule = CronScheduleBuilder
                .cronSchedule(definition.getCronExpression())
                .inTimeZone(TimeZone.getTimeZone(definition.getTimeZone()))
                .withMisfireHandlingInstructionDoNothing();
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(definition.getCode()))
                .forJob(jobKey)
                .withDescription(definition.getDisplayName())
                .withSchedule(schedule)
                .build();
    }

    private static JobKey jobKey(String code) {
        return JobKey.jobKey(code, JOB_GROUP);
    }

    private static TriggerKey triggerKey(String code) {
        return TriggerKey.triggerKey(code + ".cron", TRIGGER_GROUP);
    }
}
