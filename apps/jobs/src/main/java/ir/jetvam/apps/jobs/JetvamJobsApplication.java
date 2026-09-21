package ir.jetvam.apps.jobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ir.jetvam.apps.jobs", "ir.jetvam.modules"})
public class JetvamJobsApplication {

    public static void main(String[] args) {
        SpringApplication.run(JetvamJobsApplication.class, args);
    }
}
