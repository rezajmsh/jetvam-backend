package ir.jetvam.common.validation;

import ir.jetvam.common.text.DigitUtils;
import ir.jetvam.common.text.TextUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/** Pure validation and canonicalization for frequently used Iranian identifiers. */
public final class IranianIdentifiers {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("09\\d{9}");
    private static final Pattern NATIONAL_CODE_PATTERN = Pattern.compile("\\d{10}");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16}");
    private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("[13-9]{5}\\d{5}");
    private static final Pattern IBAN_PATTERN = Pattern.compile("IR\\d{24}");
    private static final Pattern REPEATED_DIGITS = Pattern.compile("(\\d)\\1+");

    private IranianIdentifiers() {
    }

    public static String normalizeNationalCode(CharSequence value) {
        return TextUtils.normalizeIdentifier(value);
    }

    public static boolean isValidNationalCode(CharSequence value) {
        String code = normalizeNationalCode(value);
        if (code == null || !NATIONAL_CODE_PATTERN.matcher(code).matches() || REPEATED_DIGITS.matcher(code).matches()) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.digit(code.charAt(i), 10) * (10 - i);
        }
        int remainder = sum % 11;
        int expectedCheckDigit = remainder < 2 ? remainder : 11 - remainder;
        return Character.digit(code.charAt(9), 10) == expectedCheckDigit;
    }

    public static String normalizeMobileNumber(CharSequence value) {
        String mobile = TextUtils.normalizeIdentifier(value);
        if (mobile == null) {
            return null;
        }
        mobile = mobile.replace("(", "").replace(")", "");
        if (mobile.startsWith("+98")) {
            mobile = "0" + mobile.substring(3);
        } else if (mobile.startsWith("0098")) {
            mobile = "0" + mobile.substring(4);
        } else if (mobile.startsWith("98") && mobile.length() == 12) {
            mobile = "0" + mobile.substring(2);
        } else if (mobile.startsWith("9") && mobile.length() == 10) {
            mobile = "0" + mobile;
        }
        return mobile;
    }

    public static boolean isValidMobileNumber(CharSequence value) {
        String mobile = normalizeMobileNumber(value);
        return mobile != null && MOBILE_PATTERN.matcher(mobile).matches();
    }

    public static String normalizeIban(CharSequence value) {
        if (value == null) {
            return null;
        }
        String iban = DigitUtils.toEnglishDigits(value).replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return iban.isBlank() ? null : iban;
    }

    public static boolean isValidIban(CharSequence value) {
        String iban = normalizeIban(value);
        if (iban == null || !IBAN_PATTERN.matcher(iban).matches()) {
            return false;
        }
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        int remainder = 0;
        for (int i = 0; i < rearranged.length(); i++) {
            char current = rearranged.charAt(i);
            if (Character.isDigit(current)) {
                remainder = (remainder * 10 + Character.digit(current, 10)) % 97;
            } else {
                int numeric = current - 'A' + 10;
                remainder = (remainder * 100 + numeric) % 97;
            }
        }
        return remainder == 1;
    }

    public static String normalizeBankCardNumber(CharSequence value) {
        return TextUtils.normalizeIdentifier(value);
    }

    public static boolean isValidBankCardNumber(CharSequence value) {
        String cardNumber = normalizeBankCardNumber(value);
        if (cardNumber == null
                || !BANK_CARD_PATTERN.matcher(cardNumber).matches()
                || REPEATED_DIGITS.matcher(cardNumber).matches()) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < cardNumber.length(); i++) {
            int digit = Character.digit(cardNumber.charAt(i), 10);
            int weighted = digit * (i % 2 == 0 ? 2 : 1);
            sum += weighted > 9 ? weighted - 9 : weighted;
        }
        return sum % 10 == 0;
    }

    /** Validates postal-code structure, not existence in the postal registry. */
    public static boolean isValidPostalCode(CharSequence value) {
        String postalCode = TextUtils.normalizeIdentifier(value);
        return postalCode != null
                && POSTAL_CODE_PATTERN.matcher(postalCode).matches()
                && !REPEATED_DIGITS.matcher(postalCode).matches();
    }
}
