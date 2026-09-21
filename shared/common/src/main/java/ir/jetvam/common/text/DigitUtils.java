package ir.jetvam.common.text;

public final class DigitUtils {

    private static final char[] PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹".toCharArray();
    private static final char[] ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩".toCharArray();

    private DigitUtils() {
    }

    public static String toEnglishDigits(CharSequence value) {
        if (value == null) {
            return null;
        }
        var result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            int numeric = digitValue(current);
            result.append(numeric >= 0 ? (char) ('0' + numeric) : current);
        }
        return result.toString();
    }

    public static String toPersianDigits(CharSequence value) {
        if (value == null) {
            return null;
        }
        var result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            int numeric = digitValue(current);
            result.append(numeric >= 0 ? PERSIAN_DIGITS[numeric] : current);
        }
        return result.toString();
    }

    public static boolean containsOnlyDigits(CharSequence value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (digitValue(value.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    private static int digitValue(char value) {
        if (value >= '0' && value <= '9') {
            return value - '0';
        }
        for (int i = 0; i < PERSIAN_DIGITS.length; i++) {
            if (value == PERSIAN_DIGITS[i] || value == ARABIC_DIGITS[i]) {
                return i;
            }
        }
        return -1;
    }
}
