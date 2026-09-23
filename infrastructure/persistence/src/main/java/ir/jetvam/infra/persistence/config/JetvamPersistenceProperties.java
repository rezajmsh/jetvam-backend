package ir.jetvam.infra.persistence.config;

import ir.jetvam.common.validation.Preconditions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds external settings for the jetvam persistence infrastructure.
 * Typed defaults and validation keep application configuration consistent.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

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

    /**
     * Configures Hikari connection-pool capacity and lifecycle settings.
     * Defaults provide safe PostgreSQL behavior for Jetvam applications.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
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

    /**
     * Configures Hibernate schema validation, batching and SQL diagnostics.
     * Additional provider properties can be supplied without code changes.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
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

    /**
     * Configures Flyway locations, validation and migration execution.
     * Destructive clean operations remain disabled by infrastructure code.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
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
