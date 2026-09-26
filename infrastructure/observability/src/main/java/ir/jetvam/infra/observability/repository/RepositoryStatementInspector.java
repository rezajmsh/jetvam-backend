package ir.jetvam.infra.observability.repository;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Captures SQL text for the active repository observation without recording bind values.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
public final class RepositoryStatementInspector implements StatementInspector {

    @Override
    public String inspect(String sql) {
        RepositoryTelemetryContext.recordQuery(sql);
        return sql;
    }
}
