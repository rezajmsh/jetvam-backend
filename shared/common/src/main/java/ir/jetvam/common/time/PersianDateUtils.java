package ir.jetvam.common.time;

import com.github.mfathi91.time.PersianDate;
import ir.jetvam.common.text.DigitUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Boundary around the Persian calendar implementation used by the project.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class PersianDateUtils {

    public static final DateTimeFormatter SLASH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    public static final DateTimeFormatter DASH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private PersianDateUtils() {
    }

    public static PersianDate fromGregorian(LocalDate gregorianDate) {
        return PersianDate.fromGregorian(Objects.requireNonNull(gregorianDate, "gregorianDate must not be null"));
    }

    public static LocalDate toGregorian(PersianDate persianDate) {
        return Objects.requireNonNull(persianDate, "persianDate must not be null").toGregorian();
    }

    public static PersianDate fromInstant(Instant instant, ZoneId zoneId) {
        LocalDate gregorian = Objects.requireNonNull(instant, "instant must not be null")
                .atZone(Objects.requireNonNull(zoneId, "zoneId must not be null"))
                .toLocalDate();
        return fromGregorian(gregorian);
    }

    /** Returns today's Persian date in Jetvam's default business timezone (Tehran). */
    public static PersianDate today() {
        return todayInTehran();
    }

    public static PersianDate today(ZoneId zoneId) {
        return fromGregorian(DateTimeUtils.today(zoneId));
    }

    public static PersianDate today(Clock clock, ZoneId zoneId) {
        return fromGregorian(DateTimeUtils.today(clock, zoneId));
    }

    public static PersianDate todayInTehran() {
        return fromGregorian(DateTimeUtils.todayInTehran());
    }

    public static PersianDate todayInTehran(Clock clock) {
        return today(clock, DateTimeUtils.TEHRAN_ZONE);
    }

    public static PersianDate parse(CharSequence value) {
        Objects.requireNonNull(value, "value must not be null");
        String normalized = DigitUtils.toEnglishDigits(value).strip();
        DateTimeFormatter formatter = normalized.indexOf('/') >= 0 ? SLASH_FORMATTER : DASH_FORMATTER;
        return PersianDate.parse(normalized, formatter);
    }

    public static String format(PersianDate persianDate) {
        return format(persianDate, SLASH_FORMATTER, false);
    }

    public static String format(PersianDate persianDate, boolean persianDigits) {
        return format(persianDate, SLASH_FORMATTER, persianDigits);
    }

    public static String format(PersianDate persianDate, DateTimeFormatter formatter, boolean persianDigits) {
        String formatted = Objects.requireNonNull(formatter, "formatter must not be null")
                .format(Objects.requireNonNull(persianDate, "persianDate must not be null"));
        return persianDigits ? DigitUtils.toPersianDigits(formatted) : formatted;
    }

    public static Instant toInstant(PersianDate date, LocalTime time, ZoneId zoneId) {
        return DateTimeUtils.toInstant(toGregorian(date), time, zoneId);
    }

    public static InstantRange dayRange(PersianDate date, ZoneId zoneId) {
        return DateTimeUtils.dayRange(toGregorian(date), zoneId);
    }
}
