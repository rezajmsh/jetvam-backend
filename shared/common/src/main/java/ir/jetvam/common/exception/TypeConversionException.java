package ir.jetvam.common.exception;

import java.util.List;

public final class TypeConversionException extends ValidationException {

    private final String sourceType;
    private final String targetType;

    public TypeConversionException(Object source, Class<?> targetType, Throwable cause) {
        super(
                CommonErrorCode.TYPE_CONVERSION_FAILED,
                "Cannot convert value from %s to %s".formatted(typeName(source), targetType.getName()),
                List.of(new FieldViolation("value", CommonErrorCode.TYPE_CONVERSION_FAILED.code(), "Value has an invalid type or format")),
                cause
        );
        this.sourceType = typeName(source);
        this.targetType = targetType.getName();
    }

    public String sourceType() {
        return sourceType;
    }

    public String targetType() {
        return targetType;
    }

    private static String typeName(Object source) {
        return source == null ? "null" : source.getClass().getName();
    }
}
