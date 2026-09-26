package ir.jetvam.infra.observability.repository;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryTelemetryContextTest {

    @Test
    void capturesNormalizedSqlAndHibernateTimings() {
        RepositoryTelemetryContext.Snapshot snapshot;
        try (RepositoryTelemetryContext.Scope scope = RepositoryTelemetryContext.open()) {
            RepositorySessionEventListener listener = new RepositorySessionEventListener();
            new RepositoryStatementInspector().inspect("select  *\nfrom customer where id = ?");
            listener.jdbcConnectionAcquisitionStart();
            listener.jdbcConnectionAcquisitionEnd();
            listener.jdbcPrepareStatementStart();
            listener.jdbcPrepareStatementEnd();
            listener.jdbcExecuteStatementStart();
            listener.jdbcExecuteStatementEnd();
            snapshot = scope.finish();
        }

        assertThat(snapshot.connectionAcquisitionCount()).isEqualTo(1);
        assertThat(snapshot.queries()).hasSize(1);
        assertThat(snapshot.queries().getFirst().text())
                .isEqualTo("select * from customer where id = ?");
        assertThat(snapshot.statementPreparationDuration()).isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(snapshot.statementExecutionDuration()).isGreaterThanOrEqualTo(Duration.ZERO);
    }
}
