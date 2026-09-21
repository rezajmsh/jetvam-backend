package ir.jetvam.common.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextUtilsTest {

    @Test
    void convertsAllCommonDigitSets() {
        assertEquals("0123456789", DigitUtils.toEnglishDigits("۰۱۲۳۴۵۶۷۸۹"));
        assertEquals("0123456789", DigitUtils.toEnglishDigits("٠١٢٣٤٥٦٧٨٩"));
        assertEquals("۱۲۳", DigitUtils.toPersianDigits("123"));
        assertTrue(DigitUtils.containsOnlyDigits("۱۲٣"));
        assertFalse(DigitUtils.containsOnlyDigits("12x"));
    }

    @Test
    void normalizesAndProtectsText() {
        assertNull(TextUtils.trimToNull("   "));
        assertEquals("hello world", TextUtils.normalizeWhitespace("  hello   world  "));
        assertEquals("09123456789", TextUtils.normalizeIdentifier("۰۹۱۲-۳۴۵-۶۷۸۹"));
        assertEquals("091******89", TextUtils.mask("09123456789", 3, 2));
        assertEquals("abcdefg...", TextUtils.abbreviate("abcdefghijkl", 10));
    }
}
