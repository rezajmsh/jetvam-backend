package ir.jetvam.common.convert;

import ir.jetvam.common.exception.TypeConversionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypeConversionsTest {

    private enum RequestStatus {
        IN_PROGRESS,
        COMPLETED
    }

    @Test
    void preservesNullValues() {
        assertNull(TypeConversions.asString(null));
        assertNull(TypeConversions.asInteger(null));
        assertNull(TypeConversions.asInstant(null));
    }

    @Test
    void convertsPersianDigitsAndUsesExactNumericConversions() {
        assertEquals(1234, TypeConversions.asInteger("۱۲۳۴"));
        assertEquals(42L, TypeConversions.asLong(42));
        assertEquals(new BigDecimal("19.75"), TypeConversions.asBigDecimal("۱۹.۷۵"));
        assertThrows(TypeConversionException.class, () -> TypeConversions.asInteger("1.2"));
    }

    @Test
    void convertsCommonBooleanRepresentations() {
        assertEquals(Boolean.TRUE, TypeConversions.asBoolean("yes"));
        assertEquals(Boolean.TRUE, TypeConversions.asBoolean("۱"));
        assertEquals(Boolean.FALSE, TypeConversions.asBoolean(0));
        assertThrows(TypeConversionException.class, () -> TypeConversions.asBoolean(2));
    }

    @Test
    void convertsUuidEnumAndTemporalValues() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, TypeConversions.asUuid(uuid.toString()));
        assertEquals(RequestStatus.IN_PROGRESS, TypeConversions.asEnum("in-progress", RequestStatus.class));
        assertEquals(
                Instant.parse("2026-09-21T08:00:00Z"),
                TypeConversions.asInstant("2026-09-21T11:30:00+03:30")
        );
        assertEquals(
                LocalDate.of(2026, 9, 21),
                TypeConversions.asLocalDate("۲۰۲۶-۰۹-۲۱", ZoneId.of("Asia/Tehran"))
        );
        assertEquals(
                LocalDateTime.of(2026, 9, 21, 11, 30),
                TypeConversions.asLocalDateTime("۲۰۲۶-۰۹-۲۱T۱۱:۳۰:۰۰", ZoneId.of("Asia/Tehran"))
        );
    }

    @Test
    void exposesStableConversionMetadataWithoutIncludingTheValue() {
        TypeConversionException exception = assertThrows(
                TypeConversionException.class,
                () -> TypeConversions.asUuid("secret-invalid-value")
        );

        assertEquals("COMMON.TYPE_CONVERSION_FAILED", exception.code());
        assertEquals(String.class.getName(), exception.sourceType());
        assertEquals(UUID.class.getName(), exception.targetType());
        assertNotNull(exception.getCause());
    }
}
