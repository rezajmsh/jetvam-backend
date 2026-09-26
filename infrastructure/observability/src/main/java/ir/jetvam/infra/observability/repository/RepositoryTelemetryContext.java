package ir.jetvam.infra.observability.repository;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Correlates Hibernate JDBC callbacks with the repository invocation active on the current thread.
 * SQL bind values are deliberately excluded; captured statements retain JDBC placeholders.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
public final class RepositoryTelemetryContext {

    private static final int MAX_QUERIES = 20;
    private static final int MAX_QUERY_LENGTH = 4_000;
    private static final ThreadLocal<Deque<MutableObservation>> OBSERVATIONS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private RepositoryTelemetryContext() {
    }

    public static Scope open() {
        MutableObservation observation = new MutableObservation();
        OBSERVATIONS.get().push(observation);
        return new Scope(observation);
    }

    public static boolean isActive() {
        return current() != null;
    }

    public static void recordQuery(String sql) {
        MutableObservation observation = current();
        if (observation == null || sql == null) {
            return;
        }
        observation.queryCount++;
        if (observation.queries.size() >= MAX_QUERIES) {
            return;
        }
        String normalized = sql.replaceAll("\\s+", " ").strip();
        boolean truncated = normalized.length() > MAX_QUERY_LENGTH;
        if (normalized.length() > MAX_QUERY_LENGTH) {
            normalized = normalized.substring(0, MAX_QUERY_LENGTH);
        }
        observation.queries.add(new MutableQuery(normalized, truncated));
    }

    public static void recordConnectionAcquisition(Duration duration) {
        MutableObservation observation = current();
        if (observation != null) {
            observation.connectionAcquisitionCount++;
            observation.connectionAcquisitionNanos += duration.toNanos();
        }
    }

    public static void recordStatementPreparation(Duration duration) {
        MutableObservation observation = current();
        if (observation != null) {
            observation.statementPreparationNanos += duration.toNanos();
            MutableQuery query = observation.lastQuery();
            if (query != null) {
                query.preparationNanos += duration.toNanos();
            }
        }
    }

    public static void recordStatementExecution(Duration duration) {
        MutableObservation observation = current();
        if (observation != null) {
            observation.statementExecutionNanos += duration.toNanos();
            MutableQuery query = observation.lastQuery();
            if (query != null) {
                query.executionNanos += duration.toNanos();
            }
        }
    }

    private static MutableObservation current() {
        Deque<MutableObservation> observations = OBSERVATIONS.get();
        return observations.isEmpty() ? null : observations.peek();
    }

    public static final class Scope implements AutoCloseable {

        private final MutableObservation observation;
        private boolean closed;

        private Scope(MutableObservation observation) {
            this.observation = observation;
        }

        public Snapshot finish() {
            if (closed) {
                return observation.snapshot();
            }
            Deque<MutableObservation> observations = OBSERVATIONS.get();
            if (observations.isEmpty() || observations.peek() != observation) {
                throw new IllegalStateException("Repository telemetry scopes must be closed in order");
            }
            observations.pop();
            MutableObservation parent = observations.peek();
            if (parent != null) {
                parent.merge(observation);
            }
            if (observations.isEmpty()) {
                OBSERVATIONS.remove();
            }
            closed = true;
            return observation.snapshot();
        }

        @Override
        public void close() {
            finish();
        }
    }

    public record Snapshot(
            int connectionAcquisitionCount,
            Duration connectionAcquisitionDuration,
            Duration statementPreparationDuration,
            Duration statementExecutionDuration,
            int queryCount,
            boolean queriesTruncated,
            List<Query> queries
    ) {

        public static Snapshot empty() {
            return new Snapshot(0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0, false, List.of());
        }
    }

    public record Query(
            String text,
            boolean textTruncated,
            Duration preparationDuration,
            Duration executionDuration
    ) {
    }

    private static final class MutableObservation {

        private int connectionAcquisitionCount;
        private long connectionAcquisitionNanos;
        private long statementPreparationNanos;
        private long statementExecutionNanos;
        private int queryCount;
        private final List<MutableQuery> queries = new ArrayList<>();

        private MutableQuery lastQuery() {
            return queries.isEmpty() ? null : queries.getLast();
        }

        private void merge(MutableObservation child) {
            connectionAcquisitionCount += child.connectionAcquisitionCount;
            connectionAcquisitionNanos += child.connectionAcquisitionNanos;
            statementPreparationNanos += child.statementPreparationNanos;
            statementExecutionNanos += child.statementExecutionNanos;
            queryCount += child.queryCount;
            int remaining = MAX_QUERIES - queries.size();
            if (remaining > 0) {
                queries.addAll(child.queries.subList(0, Math.min(remaining, child.queries.size())));
            }
        }

        private Snapshot snapshot() {
            return new Snapshot(
                    connectionAcquisitionCount,
                    Duration.ofNanos(connectionAcquisitionNanos),
                    Duration.ofNanos(statementPreparationNanos),
                    Duration.ofNanos(statementExecutionNanos),
                    queryCount,
                    queryCount > queries.size(),
                    queries.stream().map(MutableQuery::snapshot).toList()
            );
        }
    }

    private static final class MutableQuery {

        private final String text;
        private final boolean textTruncated;
        private long preparationNanos;
        private long executionNanos;

        private MutableQuery(String text, boolean textTruncated) {
            this.text = text;
            this.textTruncated = textTruncated;
        }

        private Query snapshot() {
            return new Query(
                    text,
                    textTruncated,
                    Duration.ofNanos(preparationNanos),
                    Duration.ofNanos(executionNanos)
            );
        }
    }
}
