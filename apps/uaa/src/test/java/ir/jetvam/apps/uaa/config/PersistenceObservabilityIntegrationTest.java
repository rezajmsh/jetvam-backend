package ir.jetvam.apps.uaa.config;

import ir.jetvam.infra.observability.repository.RepositorySessionEventListener;
import ir.jetvam.infra.observability.repository.RepositoryStatementInspector;
import ir.jetvam.infra.persistence.config.JetvamPersistenceAutoConfiguration;
import ir.jetvam.infra.persistence.config.JetvamPersistenceProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceObservabilityIntegrationTest {

    @Test
    void registersRepositorySqlAndSessionTimingInstrumentationWithHibernate() {
        Map<String, Object> hibernate = new LinkedHashMap<>();
        new JetvamPersistenceAutoConfiguration()
                .jetvamHibernatePropertiesCustomizer(new JetvamPersistenceProperties())
                .customize(hibernate);

        assertThat(hibernate)
                .containsEntry(
                        "hibernate.session_factory.statement_inspector",
                        RepositoryStatementInspector.class.getName()
                )
                .containsEntry(
                        "hibernate.session.events.auto",
                        RepositorySessionEventListener.class.getName()
                );
    }
}
