package ir.jetvam.infra.persistence.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Discovers Jetvam repositories only after a JPA entity manager is available.
 * Repository bootstrapping stays centralized without affecting JDBC-only contexts.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnBean(EntityManagerFactory.class)
@EnableJpaRepositories(basePackages = "ir.jetvam")
public class JetvamJpaRepositoriesAutoConfiguration {
}
