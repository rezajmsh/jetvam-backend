package ir.jetvam.infra.persistence.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

/**
 * Auto-configures the jetvam persistence infrastructure.
 * Applications activate reusable beans through classpath and property conditions.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@AutoConfiguration(before = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
@ConditionalOnClass({DataSource.class, HikariDataSource.class})
@ConditionalOnProperty(prefix = "jetvam.persist", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JetvamPersistenceProperties.class)
@EntityScan(basePackages = "ir.jetvam")
public class JetvamPersistenceAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(DataSource.class)
    public HikariDataSource jetvamDataSource(JetvamPersistenceProperties properties) {
        properties.validate();
        JetvamPersistenceProperties.Pool pool = properties.getPool();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        if (StringUtils.hasText(properties.getDriverClassName())) {
            config.setDriverClassName(properties.getDriverClassName());
        }
        config.setSchema(properties.getDefaultSchema());
        config.setReadOnly(properties.isReadOnly());
        config.setAutoCommit(properties.isAutoCommit());
        config.setPoolName(pool.getName());
        config.setMinimumIdle(pool.getMinimumIdle());
        config.setMaximumPoolSize(pool.getMaximumPoolSize());
        config.setConnectionTimeout(pool.getConnectionTimeout().toMillis());
        config.setValidationTimeout(pool.getValidationTimeout().toMillis());
        config.setIdleTimeout(pool.getIdleTimeout().toMillis());
        config.setMaxLifetime(pool.getMaxLifetime().toMillis());
        config.setKeepaliveTime(pool.getKeepaliveTime().toMillis());
        config.setLeakDetectionThreshold(pool.getLeakDetectionThreshold().toMillis());
        config.setInitializationFailTimeout(pool.getInitializationFailTimeout());
        config.setRegisterMbeans(pool.isRegisterMbeans());
        if (StringUtils.hasText(pool.getConnectionTestQuery())) {
            config.setConnectionTestQuery(pool.getConnectionTestQuery());
        }
        if (StringUtils.hasText(pool.getTransactionIsolation())) {
            config.setTransactionIsolation(pool.getTransactionIsolation());
        }
        Properties dataSourceProperties = new Properties();
        dataSourceProperties.putAll(properties.getDataSourceProperties());
        config.setDataSourceProperties(dataSourceProperties);
        return new HikariDataSource(config);
    }

    @Bean
    @ConditionalOnClass(HibernatePropertiesCustomizer.class)
    public HibernatePropertiesCustomizer jetvamHibernatePropertiesCustomizer(
            JetvamPersistenceProperties properties
    ) {
        return hibernate -> customizeHibernate(hibernate, properties);
    }

    @Bean
    @ConditionalOnClass(Flyway.class)
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway jetvamFlyway(DataSource dataSource, JetvamPersistenceProperties properties) {
        JetvamPersistenceProperties.Migration migration = properties.getMigration();
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .defaultSchema(properties.getDefaultSchema())
                .schemas(properties.getDefaultSchema())
                .createSchemas(true)
                .locations(migration.getLocations().toArray(String[]::new))
                .baselineOnMigrate(migration.isBaselineOnMigrate())
                .baselineVersion(MigrationVersion.fromVersion(migration.getBaselineVersion()))
                .validateOnMigrate(migration.isValidateOnMigrate())
                .validateMigrationNaming(migration.isValidateMigrationNaming())
                .outOfOrder(migration.isOutOfOrder())
                .lockRetryCount(migration.getLockRetryCount())
                .cleanDisabled(true);
        return configuration.load();
    }

    @Bean
    @ConditionalOnClass(FlywayMigrationInitializer.class)
    @ConditionalOnMissingBean(FlywayMigrationStrategy.class)
    public FlywayMigrationStrategy jetvamFlywayMigrationStrategy(JetvamPersistenceProperties properties) {
        return flyway -> {
            if (properties.getMigration().isEnabled()) {
                flyway.migrate();
            }
        };
    }

    @Bean
    @ConditionalOnClass(FlywayMigrationInitializer.class)
    @ConditionalOnMissingBean(FlywayMigrationInitializer.class)
    public FlywayMigrationInitializer jetvamFlywayMigrationInitializer(
            Flyway flyway,
            FlywayMigrationStrategy strategy
    ) {
        return new FlywayMigrationInitializer(flyway, strategy);
    }

    private static void customizeHibernate(
            Map<String, Object> hibernate,
            JetvamPersistenceProperties properties
    ) {
        JetvamPersistenceProperties.Jpa jpa = properties.getJpa();
        hibernate.put("hibernate.default_schema", properties.getDefaultSchema());
        hibernate.put("hibernate.hbm2ddl.auto", jpa.getDdlAuto());
        hibernate.put("hibernate.show_sql", jpa.isShowSql());
        hibernate.put("hibernate.format_sql", jpa.isFormatSql());
        hibernate.put("hibernate.generate_statistics", jpa.isGenerateStatistics());
        hibernate.put("hibernate.jdbc.time_zone", jpa.getJdbcTimeZone());
        hibernate.put("hibernate.jdbc.batch_size", jpa.getJdbcBatchSize());
        hibernate.put("hibernate.order_inserts", jpa.isOrderInserts());
        hibernate.put("hibernate.order_updates", jpa.isOrderUpdates());
        hibernate.putAll(jpa.getProperties());
    }
}
