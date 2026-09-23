package ir.jetvam.common.convert;

import ir.jetvam.common.exception.TypeConversionException;
import ir.jetvam.common.text.DigitUtils;
import ir.jetvam.common.text.TextUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Strict, null-preserving conversions for data received from external boundaries.
 * A {@code null} input always produces {@code null}; invalid non-null values fail fast.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class TypeConversions {

    private TypeConversions() {
    }

    public static String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return value.toString();
    }

    public static String asNonBlankString(Object value) {
        return TextUtils.trimToNull(asString(value));
    }

    public static Integer asInteger(Object value) {
        return convert(value, Integer.class, input -> asBigDecimalInternal(input).intValueExact());
    }

    public static Long asLong(Object value) {
        return convert(value, Long.class, input -> asBigDecimalInternal(input).longValueExact());
    }

    public static BigInteger asBigInteger(Object value) {
        return convert(value, BigInteger.class, input -> asBigDecimalInternal(input).toBigIntegerExact());
    }

    public static BigDecimal asBigDecimal(Object value) {
        return convert(value, BigDecimal.class, TypeConversions::asBigDecimalInternal);
    }

    public static Boolean asBoolean(Object value) {
        return convert(value, Boolean.class, input -> {
            if (input instanceof Boolean booleanValue) {
                return booleanValue;
            }
            if (input instanceof Number number) {
                int intValue = asBigDecimalInternal(number).intValueExact();
                if (intValue == 0 || intValue == 1) {
                    return intValue == 1;
                }
                throw new IllegalArgumentException("Only zero and one are valid numeric booleans");
            }
            String normalized = DigitUtils.toEnglishDigits(input.toString()).strip().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "true", "1", "yes", "y", "on" -> true;
                case "false", "0", "no", "n", "off" -> false;
                default -> throw new IllegalArgumentException("Unsupported boolean value");
            };
        });
    }

    public static UUID asUuid(Object value) {
        return convert(value, UUID.class, input -> input instanceof UUID uuid
                ? uuid
                : UUID.fromString(input.toString().strip()));
    }

    public static <E extends Enum<E>> E asEnum(Object value, Class<E> enumType) {
        if (value == null) {
            return null;
        }
        try {
            if (enumType.isInstance(value)) {
                return enumType.cast(value);
            }
            String candidate = value.toString().strip().replace('-', '_').replace(' ', '_');
            for (E constant : enumType.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(candidate)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("Unknown enum constant");
        } catch (RuntimeException exception) {
            throw conversionFailure(value, enumType, exception);
        }
    }

    /** Numeric inputs are treated as epoch milliseconds. */
    public static Instant asInstant(Object value) {
        return convert(value, Instant.class, input -> {
            if (input instanceof Instant instant) {
                return instant;
            }
            if (input instanceof Date date) {
                return date.toInstant();
            }
            if (input instanceof Number number) {
                return Instant.ofEpochMilli(asBigDecimalInternal(number).longValueExact());
            }
            String text = input.toString().strip();
            try {
                return Instant.parse(text);
            } catch (RuntimeException ignored) {
                try {
                    return OffsetDateTime.parse(text).toInstant();
                } catch (RuntimeException ignoredAgain) {
                    return ZonedDateTime.parse(text).toInstant();
                }
            }
        });
    }

    public static LocalDate asLocalDate(Object value, ZoneId zoneId) {
        return convert(value, LocalDate.class, input -> {
            if (input instanceof LocalDate date) {
                return date;
            }
            if (input instanceof LocalDateTime dateTime) {
                return dateTime.toLocalDate();
            }
            if (input instanceof Instant instant) {
                return instant.atZone(zoneId).toLocalDate();
            }
            if (input instanceof Date date) {
                return date.toInstant().atZone(zoneId).toLocalDate();
            }
            return LocalDate.parse(DigitUtils.toEnglishDigits(input.toString()).strip());
        });
    }

    public static LocalDateTime asLocalDateTime(Object value, ZoneId zoneId) {
        return convert(value, LocalDateTime.class, input -> {
            if (input instanceof LocalDateTime dateTime) {
                return dateTime;
            }
            if (input instanceof LocalDate date) {
                return date.atStartOfDay();
            }
            if (input instanceof Instant instant) {
                return LocalDateTime.ofInstant(instant, zoneId);
            }
            if (input instanceof Date date) {
                return LocalDateTime.ofInstant(date.toInstant(), zoneId);
            }
            return LocalDateTime.parse(DigitUtils.toEnglishDigits(input.toString()).strip());
        });
    }

    private static BigDecimal asBigDecimalInternal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        if (value instanceof Float || value instanceof Double) {
            double doubleValue = ((Number) value).doubleValue();
            if (!Double.isFinite(doubleValue)) {
                throw new NumberFormatException("Non-finite number");
            }
            return BigDecimal.valueOf(doubleValue);
        }
        String text = DigitUtils.toEnglishDigits(value.toString()).strip();
        return new BigDecimal(text);
    }

    private static <T> T convert(Object value, Class<T> targetType, ConversionOperation<T> operation) {
        if (value == null) {
            return null;
        }
        try {
            return operation.convert(value);
        } catch (TypeConversionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw conversionFailure(value, targetType, exception);
        }
    }

    private static TypeConversionException conversionFailure(Object value, Class<?> targetType, RuntimeException cause) {
        return new TypeConversionException(value, targetType, cause);
    }

    @FunctionalInterface
    /**
     * Encapsulates one conversion operation that may fail with a runtime cause.
     * The wrapper converts failures to the common conversion exception.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    private interface ConversionOperation<T> {
        T convert(Object value);
    }
}
