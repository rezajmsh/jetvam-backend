package ir.jetvam.common.text;

import java.util.regex.Pattern;

/**
 * Provides null-safe text normalization and presence operations.
 * Common conversion and validation code reuses these rules.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class TextUtils {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern IDENTIFIER_SEPARATORS = Pattern.compile("[\\s-]+");

    private TextUtils() {
    }

    public static boolean isBlank(CharSequence value) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasText(CharSequence value) {
        return !isBlank(value);
    }

    public static String trimToNull(CharSequence value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.toString().strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normalizeWhitespace(CharSequence value) {
        String text = trimToNull(value);
        return text == null ? null : WHITESPACE.matcher(text).replaceAll(" ");
    }

    public static String normalizeIdentifier(CharSequence value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        return IDENTIFIER_SEPARATORS.matcher(DigitUtils.toEnglishDigits(text)).replaceAll("");
    }

    public static String abbreviate(CharSequence value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength < 4) {
            throw new IllegalArgumentException("maxLength must be at least 4");
        }
        String text = value.toString();
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    public static String mask(CharSequence value, int visiblePrefix, int visibleSuffix) {
        if (value == null) {
            return null;
        }
        if (visiblePrefix < 0 || visibleSuffix < 0) {
            throw new IllegalArgumentException("Visible character counts must not be negative");
        }
        String text = value.toString();
        int visible = visiblePrefix + visibleSuffix;
        if (text.length() <= visible) {
            return "*".repeat(text.length());
        }
        return text.substring(0, visiblePrefix)
                + "*".repeat(text.length() - visible)
                + text.substring(text.length() - visibleSuffix);
    }
}
