package ir.jetvam.infra.persistence.config;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JetvamPersistenceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JetvamPersistenceAutoConfiguration.class,
                    DataSourceAutoConfiguration.class,
                    FlywayAutoConfiguration.class
            ))
            .withPropertyValues(
                    "jetvam.persist.url=jdbc:postgresql://localhost:5432/jetvam_test",
                    "jetvam.persist.username=test_user",
                    "jetvam.persist.password=test_password",
                    "jetvam.persist.default-schema=test_schema",
                    "jetvam.persist.pool.name=test-pool",
                    "jetvam.persist.pool.minimum-idle=1",
                    "jetvam.persist.pool.maximum-pool-size=7",
                    "jetvam.persist.pool.initialization-fail-timeout=-1",
                    "jetvam.persist.jpa.show-sql=true",
                    "jetvam.persist.migration.enabled=false"
            );

    @Test
    void createsDataSourceFlywayAndHibernateSettingsFromJetvamProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            HikariDataSource dataSource = context.getBean(HikariDataSource.class);
            assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/jetvam_test");
            assertThat(dataSource.getUsername()).isEqualTo("test_user");
            assertThat(dataSource.getSchema()).isEqualTo("test_schema");
            assertThat(dataSource.getMaximumPoolSize()).isEqualTo(7);
            assertThat(context).hasSingleBean(Flyway.class);

            Map<String, Object> hibernate = new LinkedHashMap<>();
            context.getBean(HibernatePropertiesCustomizer.class).customize(hibernate);
            assertThat(hibernate)
                    .containsEntry("hibernate.default_schema", "test_schema")
                    .containsEntry("hibernate.hbm2ddl.auto", "validate")
                    .containsEntry("hibernate.show_sql", true)
                    .containsEntry("hibernate.jdbc.time_zone", "UTC");
        });
    }
}
