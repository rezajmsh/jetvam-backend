package ir.jetvam.common.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the behavior of clock time provider.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class ClockTimeProviderTest {

    @Test
    void derivesAllCurrentValuesFromTheSuppliedClock() {
        var instant = Instant.parse("2026-03-20T21:00:00Z");
        var provider = new ClockTimeProvider(Clock.fixed(instant, ZoneOffset.UTC));

        assertEquals(instant, provider.now());
        assertEquals(LocalDate.of(2026, 3, 21), provider.todayInTehran());
        assertEquals("1405-01-01", provider.currentPersianDate().toString());
        assertEquals(instant.toEpochMilli(), provider.currentEpochMillis());
    }
}
