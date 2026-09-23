package ir.jetvam.common.time;

import com.github.mfathi91.time.PersianDate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Application-wide source of current time.
 *
 * <p>Application code should depend on this abstraction instead of calling
 * {@code Instant.now()} or creating a {@link Clock}. The underlying clock can
 * then be replaced once for tests or special runtime environments.</p>
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface TimeProvider {

    Clock clock();

    default Instant now() {
        return DateTimeUtils.now(clock());
    }

    default long currentEpochMillis() {
        return now().toEpochMilli();
    }

    default LocalDate today() {
        return todayInTehran();
    }

    default LocalDate today(ZoneId zoneId) {
        return DateTimeUtils.today(clock(), zoneId);
    }

    default LocalDate todayInTehran() {
        return DateTimeUtils.todayInTehran(clock());
    }

    default ZonedDateTime zonedNow(ZoneId zoneId) {
        return DateTimeUtils.zonedNow(clock(), zoneId);
    }

    default ZonedDateTime nowInTehran() {
        return DateTimeUtils.nowInTehran(clock());
    }

    default LocalDateTime currentDateTime(ZoneId zoneId) {
        return DateTimeUtils.currentDateTime(clock(), zoneId);
    }

    default LocalDateTime currentDateTimeInTehran() {
        return DateTimeUtils.currentDateTimeInTehran(clock());
    }

    default LocalTime currentTime(ZoneId zoneId) {
        return DateTimeUtils.currentTime(clock(), zoneId);
    }

    default LocalTime currentTimeInTehran() {
        return DateTimeUtils.currentTimeInTehran(clock());
    }

    default PersianDate currentPersianDate() {
        return PersianDateUtils.todayInTehran(clock());
    }

    default PersianDate currentPersianDate(ZoneId zoneId) {
        return PersianDateUtils.today(clock(), zoneId);
    }
}
