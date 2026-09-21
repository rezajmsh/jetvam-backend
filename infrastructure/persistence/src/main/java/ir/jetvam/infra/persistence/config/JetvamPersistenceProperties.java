package ir.jetvam.infra.persistence.config;

import ir.jetvam.common.validation.Preconditions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("jetvam.persist")
@Getter
@Setter
public class JetvamPersistenceProperties {

    private boolean enabled = true;
    private String url = "jdbc:postgresql://localhost:5432/jetvam";
    private String username = "jetvam";
    private String password = "";
    private String driverClassName = "org.postgresql.Driver";
    private String defaultSchema = "jetvam";
    private boolean readOnly;
    private boolean autoCommit = true;
    private final Pool pool = new Pool();
    private final Jpa jpa = new Jpa();
    private final Migration migration = new Migration();
    private final Map<String, String> dataSourceProperties = new LinkedHashMap<>();

    public void validate() {
        Preconditions.requireText(url, "jetvam.persist.url");
        Preconditions.requireText(username, "jetvam.persist.username");
        Preconditions.requireText(defaultSchema, "jetvam.persist.default-schema");
        Preconditions.require(
                pool.minimumIdle >= 0
                        && pool.maximumPoolSize >= 1
                        && pool.minimumIdle <= pool.maximumPoolSize,
                "jetvam.persist.pool requires 0 <= minimum-idle <= maximum-pool-size"
        );
    }

    @Getter
    @Setter
    public static class Pool {
        private String name = "jetvam-pool";
        private int minimumIdle = 5;
        private int maximumPoolSize = 30;
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration validationTimeout = Duration.ofSeconds(5);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private Duration maxLifetime = Duration.ofMinutes(30);
        private Duration keepaliveTime = Duration.ZERO;
        private Duration leakDetectionThreshold = Duration.ZERO;
        private long initializationFailTimeout = 1;
        private String connectionTestQuery;
        private String transactionIsolation;
        private boolean registerMbeans;

    }

    @Getter
    @Setter
    public static class Jpa {
        private String ddlAuto = "validate";
        private boolean showSql;
        private boolean formatSql;
        private boolean generateStatistics;
        private String jdbcTimeZone = "UTC";
        private int jdbcBatchSize = 50;
        private boolean orderInserts = true;
        private boolean orderUpdates = true;
        private final Map<String, Object> properties = new LinkedHashMap<>();

    }

    @Getter
    @Setter
    public static class Migration {
        private boolean enabled = true;
        private List<String> locations = List.of("classpath:db/migration");
        private boolean baselineOnMigrate;
        private String baselineVersion = "0";
        private boolean validateOnMigrate = true;
        private boolean validateMigrationNaming = true;
        private boolean outOfOrder;
        private int lockRetryCount = 50;

    }
}
