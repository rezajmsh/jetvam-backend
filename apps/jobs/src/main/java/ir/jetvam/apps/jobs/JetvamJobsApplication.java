package ir.jetvam.apps.jobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstraps the Jetvam background-jobs application.
 * It hosts scheduled and asynchronous operational workloads.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@SpringBootApplication(scanBasePackages = {"ir.jetvam.apps.jobs", "ir.jetvam.modules"})
public class JetvamJobsApplication {

    public static void main(String[] args) {
        SpringApplication.run(JetvamJobsApplication.class, args);
    }
}
