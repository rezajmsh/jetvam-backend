package ir.jetvam.common.time;

import com.github.mfathi91.time.PersianDate;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies the behavior of persian date utils.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class PersianDateUtilsTest {

    @Test
    void providesCurrentPersianDateWithoutRequiringAClock() {
        assertNotNull(PersianDateUtils.today());
        assertEquals(
                PersianDateUtils.fromGregorian(DateTimeUtils.todayInTehran()),
                PersianDateUtils.todayInTehran()
        );
    }

    @Test
    void convertsBetweenPersianAndGregorianDates() {
        PersianDate persianDate = PersianDateUtils.parse("۱۴۰۳/۰۱/۰۱");

        assertEquals(LocalDate.of(2024, 3, 20), PersianDateUtils.toGregorian(persianDate));
        assertEquals(persianDate, PersianDateUtils.fromGregorian(LocalDate.of(2024, 3, 20)));
        assertEquals("1403/01/01", PersianDateUtils.format(persianDate));
        assertEquals("۱۴۰۳/۰۱/۰۱", PersianDateUtils.format(persianDate, true));
    }

    @Test
    void calculatesTodayFromInjectedClock() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-21T00:00:00Z"), ZoneOffset.UTC);

        assertEquals("1405/01/01", PersianDateUtils.format(PersianDateUtils.todayInTehran(clock)));
    }
}
