package ir.jetvam.common.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Half-open instant range: start is included and end is excluded. */
public record InstantRange(Instant startInclusive, Instant endExclusive) {

    public InstantRange {
        Objects.requireNonNull(startInclusive, "startInclusive must not be null");
        Objects.requireNonNull(endExclusive, "endExclusive must not be null");
        if (endExclusive.isBefore(startInclusive)) {
            throw new IllegalArgumentException("endExclusive must not be before startInclusive");
        }
    }

    public boolean contains(Instant instant) {
        Objects.requireNonNull(instant, "instant must not be null");
        return !instant.isBefore(startInclusive) && instant.isBefore(endExclusive);
    }

    public boolean overlaps(InstantRange other) {
        Objects.requireNonNull(other, "other must not be null");
        return startInclusive.isBefore(other.endExclusive) && other.startInclusive.isBefore(endExclusive);
    }

    public Duration duration() {
        return Duration.between(startInclusive, endExclusive);
    }

    public boolean isEmpty() {
        return startInclusive.equals(endExclusive);
    }
}
