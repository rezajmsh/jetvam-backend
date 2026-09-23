package ir.jetvam.apps.jobs.infrastructure.handler;

/**
 * Business-neutral extension point implemented by each scheduled workload.
 * Implementations should delegate to the module that owns the use case.
 */
public interface JobHandler {

    String key();

    JobResult execute(JobContext context) throws Exception;
}
