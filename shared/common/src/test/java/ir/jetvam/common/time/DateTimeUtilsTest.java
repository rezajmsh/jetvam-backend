package ir.jetvam.common.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the behavior of date time utils.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class DateTimeUtilsTest {

    @Test
    void providesCurrentSystemTimeWithoutRequiringAClock() {
        Instant before = Instant.now().minusSeconds(1);
        Instant actual = DateTimeUtils.now();
        Instant after = Instant.now().plusSeconds(1);

        assertFalse(actual.isBefore(before));
        assertFalse(actual.isAfter(after));
        assertEquals(actual.toEpochMilli(), DateTimeUtils.toEpochMillis(actual));
    }

    @Test
    void providesConvenientCurrentTehranValues() {
        LocalDateTime expected = LocalDateTime.ofInstant(DateTimeUtils.now(), DateTimeUtils.TEHRAN_ZONE);
        LocalDate actualDate = DateTimeUtils.today();
        LocalDateTime actualDateTime = DateTimeUtils.currentDateTimeInTehran();
        LocalTime actualTime = DateTimeUtils.currentTimeInTehran();

        assertEquals(DateTimeUtils.TEHRAN_ZONE, DateTimeUtils.nowInTehran().getZone());
        assertTrue(Math.abs(Duration.between(expected, actualDateTime).toSeconds()) <= 1);
        assertEquals(actualDateTime.toLocalDate(), actualDate);
        assertTrue(Math.abs(Duration.between(actualDateTime.toLocalTime(), actualTime).toSeconds()) <= 1);
    }

    @Test
    void usesExplicitClockAndTehranZone() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-20T21:00:00Z"), ZoneOffset.UTC);

        assertEquals(LocalDate.of(2026, 9, 21), DateTimeUtils.todayInTehran(clock));
        assertEquals(Instant.parse("2026-09-20T21:00:00Z"), DateTimeUtils.now(clock));
        assertEquals(LocalDateTime.of(2026, 9, 21, 0, 30), DateTimeUtils.currentDateTimeInTehran(clock));
        assertEquals(LocalTime.of(0, 30), DateTimeUtils.currentTimeInTehran(clock));
    }

    @Test
    void createsHalfOpenDayRanges() {
        InstantRange range = DateTimeUtils.dayRange(LocalDate.of(2026, 9, 21), DateTimeUtils.TEHRAN_ZONE);

        assertEquals(Instant.parse("2026-09-20T20:30:00Z"), range.startInclusive());
        assertEquals(Instant.parse("2026-09-21T20:30:00Z"), range.endExclusive());
        assertEquals(Duration.ofHours(24), range.duration());
        assertTrue(range.contains(range.startInclusive()));
        assertFalse(range.contains(range.endExclusive()));
    }

    @Test
    void supportsInclusiveDateRanges() {
        DateRange first = new DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));
        DateRange second = new DateRange(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 5));

        assertEquals(3, first.lengthInDays());
        assertTrue(first.overlaps(second));
        assertTrue(first.contains(LocalDate.of(2026, 9, 2)));
    }

    @Test
    void parsesAndFormatsIsoInstants() {
        Instant instant = DateTimeUtils.parseInstant("2026-09-21T11:30:00+03:30");
        assertEquals("2026-09-21T08:00:00Z", DateTimeUtils.formatInstant(instant));
        assertEquals("2026-09-21T08:00:00.000Z", DateTimeUtils.formatInstantMillis(instant));
    }
}
