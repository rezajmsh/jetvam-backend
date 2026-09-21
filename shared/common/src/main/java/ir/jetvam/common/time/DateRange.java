package ir.jetvam.common.time;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Inclusive local-date range. */
public record DateRange(LocalDate startInclusive, LocalDate endInclusive) {

    public DateRange {
        Objects.requireNonNull(startInclusive, "startInclusive must not be null");
        Objects.requireNonNull(endInclusive, "endInclusive must not be null");
        if (endInclusive.isBefore(startInclusive)) {
            throw new IllegalArgumentException("endInclusive must not be before startInclusive");
        }
    }

    public boolean contains(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return !date.isBefore(startInclusive) && !date.isAfter(endInclusive);
    }

    public boolean overlaps(DateRange other) {
        Objects.requireNonNull(other, "other must not be null");
        return !endInclusive.isBefore(other.startInclusive) && !other.endInclusive.isBefore(startInclusive);
    }

    public long lengthInDays() {
        return ChronoUnit.DAYS.between(startInclusive, endInclusive) + 1;
    }
}
