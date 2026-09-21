package ir.jetvam.apps.uaa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ir.jetvam.apps.uaa", "ir.jetvam.modules.identity"})
public class JetvamUaaApplication {

    public static void main(String[] args) {
        SpringApplication.run(JetvamUaaApplication.class, args);
    }
}
