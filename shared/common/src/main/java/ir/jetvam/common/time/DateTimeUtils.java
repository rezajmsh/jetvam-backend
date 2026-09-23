package ir.jetvam.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Provides conversion and calculation helpers for Java date-time types.
 * Current values use the centralized Jetvam UTC clock and Tehran zone.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class DateTimeUtils {

    public static final ZoneId TEHRAN_ZONE = ZoneId.of("Asia/Tehran");
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final Clock SYSTEM_CLOCK = JetvamClocks.systemUtc();
    public static final DateTimeFormatter ISO_INSTANT_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(UTC_ZONE);

    private DateTimeUtils() {
    }

    /** Returns the current point on the global timeline. */
    public static Instant now() {
        return now(SYSTEM_CLOCK);
    }

    public static Instant now(Clock clock) {
        return Objects.requireNonNull(clock, "clock must not be null").instant();
    }

    /** Returns the current Gregorian date in Jetvam's default business timezone (Tehran). */
    public static LocalDate today() {
        return todayInTehran();
    }

    public static LocalDate today(ZoneId zoneId) {
        return today(SYSTEM_CLOCK, zoneId);
    }

    public static LocalDate today(Clock clock, ZoneId zoneId) {
        return LocalDate.now(Objects.requireNonNull(clock, "clock must not be null").withZone(requireZone(zoneId)));
    }

    public static LocalDate todayInTehran() {
        return todayInTehran(SYSTEM_CLOCK);
    }

    public static LocalDate todayInTehran(Clock clock) {
        return today(clock, TEHRAN_ZONE);
    }

    public static ZonedDateTime nowInTehran() {
        return nowInTehran(SYSTEM_CLOCK);
    }

    public static ZonedDateTime nowInTehran(Clock clock) {
        return zonedNow(clock, TEHRAN_ZONE);
    }

    public static ZonedDateTime zonedNow(ZoneId zoneId) {
        return zonedNow(SYSTEM_CLOCK, zoneId);
    }

    public static ZonedDateTime zonedNow(Clock clock, ZoneId zoneId) {
        return ZonedDateTime.now(
                Objects.requireNonNull(clock, "clock must not be null").withZone(requireZone(zoneId))
        );
    }

    public static LocalDateTime currentDateTime(ZoneId zoneId) {
        return currentDateTime(SYSTEM_CLOCK, zoneId);
    }

    public static LocalDateTime currentDateTime(Clock clock, ZoneId zoneId) {
        return zonedNow(clock, zoneId).toLocalDateTime();
    }

    public static LocalDateTime currentDateTimeInTehran() {
        return currentDateTimeInTehran(SYSTEM_CLOCK);
    }

    public static LocalDateTime currentDateTimeInTehran(Clock clock) {
        return currentDateTime(clock, TEHRAN_ZONE);
    }

    public static LocalTime currentTime(ZoneId zoneId) {
        return currentTime(SYSTEM_CLOCK, zoneId);
    }

    public static LocalTime currentTime(Clock clock, ZoneId zoneId) {
        return zonedNow(clock, zoneId).toLocalTime();
    }

    public static LocalTime currentTimeInTehran() {
        return currentTimeInTehran(SYSTEM_CLOCK);
    }

    public static LocalTime currentTimeInTehran(Clock clock) {
        return currentTime(clock, TEHRAN_ZONE);
    }

    public static long currentEpochMillis() {
        return now().toEpochMilli();
    }

    public static Instant toInstant(LocalDateTime dateTime, ZoneId zoneId) {
        return Objects.requireNonNull(dateTime, "dateTime must not be null")
                .atZone(requireZone(zoneId))
                .toInstant();
    }

    public static Instant toInstant(LocalDate date, LocalTime time, ZoneId zoneId) {
        return toInstant(
                LocalDateTime.of(
                        Objects.requireNonNull(date, "date must not be null"),
                        Objects.requireNonNull(time, "time must not be null")
                ),
                zoneId
        );
    }

    public static LocalDateTime toLocalDateTime(Instant instant, ZoneId zoneId) {
        return LocalDateTime.ofInstant(
                Objects.requireNonNull(instant, "instant must not be null"),
                requireZone(zoneId)
        );
    }

    public static ZonedDateTime toTehranDateTime(Instant instant) {
        return Objects.requireNonNull(instant, "instant must not be null").atZone(TEHRAN_ZONE);
    }

    public static Instant startOfDay(LocalDate date, ZoneId zoneId) {
        return Objects.requireNonNull(date, "date must not be null")
                .atStartOfDay(requireZone(zoneId))
                .toInstant();
    }

    public static Instant endOfDayExclusive(LocalDate date, ZoneId zoneId) {
        return startOfDay(Objects.requireNonNull(date, "date must not be null").plusDays(1), zoneId);
    }

    public static InstantRange dayRange(LocalDate date, ZoneId zoneId) {
        return new InstantRange(startOfDay(date, zoneId), endOfDayExclusive(date, zoneId));
    }

    public static InstantRange toInstantRange(DateRange dateRange, ZoneId zoneId) {
        Objects.requireNonNull(dateRange, "dateRange must not be null");
        return new InstantRange(
                startOfDay(dateRange.startInclusive(), zoneId),
                endOfDayExclusive(dateRange.endInclusive(), zoneId)
        );
    }

    public static Instant parseInstant(String value) {
        Objects.requireNonNull(value, "value must not be null");
        String text = value.strip();
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(text).toInstant();
            } catch (DateTimeParseException ignoredAgain) {
                return ZonedDateTime.parse(text).toInstant();
            }
        }
    }

    public static String formatInstant(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(Objects.requireNonNull(instant, "instant must not be null"));
    }

    public static String formatInstantMillis(Instant instant) {
        return ISO_INSTANT_MILLIS.format(Objects.requireNonNull(instant, "instant must not be null"));
    }

    public static Instant fromEpochMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis);
    }

    public static long toEpochMillis(Instant instant) {
        return Objects.requireNonNull(instant, "instant must not be null").toEpochMilli();
    }

    public static Instant truncateToSeconds(Instant instant) {
        return Objects.requireNonNull(instant, "instant must not be null").truncatedTo(ChronoUnit.SECONDS);
    }

    private static ZoneId requireZone(ZoneId zoneId) {
        return Objects.requireNonNull(zoneId, "zoneId must not be null");
    }
}
