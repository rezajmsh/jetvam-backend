package ir.jetvam.infra.observability.repository;

import org.hibernate.SessionEventListener;

import java.time.Duration;

/**
 * Captures Hibernate connection acquisition and JDBC statement timings for repository logs.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
public final class RepositorySessionEventListener implements SessionEventListener {

    private transient long connectionAcquisitionStartedAt;
    private transient long statementPreparationStartedAt;
    private transient long statementExecutionStartedAt;

    @Override
    public void jdbcConnectionAcquisitionStart() {
        connectionAcquisitionStartedAt = RepositoryTelemetryContext.isActive() ? System.nanoTime() : 0;
    }

    @Override
    public void jdbcConnectionAcquisitionEnd() {
        if (connectionAcquisitionStartedAt != 0) {
            RepositoryTelemetryContext.recordConnectionAcquisition(elapsed(connectionAcquisitionStartedAt));
            connectionAcquisitionStartedAt = 0;
        }
    }

    @Override
    public void jdbcPrepareStatementStart() {
        statementPreparationStartedAt = RepositoryTelemetryContext.isActive() ? System.nanoTime() : 0;
    }

    @Override
    public void jdbcPrepareStatementEnd() {
        if (statementPreparationStartedAt != 0) {
            RepositoryTelemetryContext.recordStatementPreparation(elapsed(statementPreparationStartedAt));
            statementPreparationStartedAt = 0;
        }
    }

    @Override
    public void jdbcExecuteStatementStart() {
        statementExecutionStartedAt = RepositoryTelemetryContext.isActive() ? System.nanoTime() : 0;
    }

    @Override
    public void jdbcExecuteStatementEnd() {
        if (statementExecutionStartedAt != 0) {
            RepositoryTelemetryContext.recordStatementExecution(elapsed(statementExecutionStartedAt));
            statementExecutionStartedAt = 0;
        }
    }

    @Override
    public void jdbcExecuteBatchStart() {
        jdbcExecuteStatementStart();
    }

    @Override
    public void jdbcExecuteBatchEnd() {
        jdbcExecuteStatementEnd();
    }

    private static Duration elapsed(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }
}
