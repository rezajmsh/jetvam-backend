package ir.jetvam.common.validation;

import ir.jetvam.common.validation.validator.IranianBankCardValidator;
import ir.jetvam.common.validation.validator.IranianMobileNumberValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IranianIdentifiersTest {

    @Test
    void validatesNationalCodeWithPersianDigits() {
        assertTrue(IranianIdentifiers.isValidNationalCode("۰۰۶۷۷۴۹۸۲۸"));
        assertFalse(IranianIdentifiers.isValidNationalCode("1111111111"));
        assertFalse(IranianIdentifiers.isValidNationalCode("0067749827"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"09123456789", "+98 912-345-6789", "00989123456789", "۹۱۲۳۴۵۶۷۸۹"})
    void normalizesAndValidatesMobileNumbers(String value) {
        assertEquals("09123456789", IranianIdentifiers.normalizeMobileNumber(value));
        assertTrue(IranianIdentifiers.isValidMobileNumber(value));
    }

    @Test
    void validatesIbanUsingMod97() {
        assertTrue(IranianIdentifiers.isValidIban("IR82 0540 1026 8002 0817 9090 02"));
        assertFalse(IranianIdentifiers.isValidIban("IR83 0540 1026 8002 0817 9090 02"));
    }

    @Test
    void validatesBankCardWithLuhnAndPostalCodeStructure() {
        assertTrue(IranianIdentifiers.isValidBankCardNumber("6037-9975-1234-5670"));
        assertFalse(IranianIdentifiers.isValidBankCardNumber("6037-9975-1234-5678"));
        assertTrue(IranianIdentifiers.isValidPostalCode("13456-78910"));
        assertFalse(IranianIdentifiers.isValidPostalCode("10456-78910"));
    }

    @Test
    void beanValidatorsTreatNullAsOptional() {
        assertTrue(new IranianMobileNumberValidator().isValid(null, null));
        assertTrue(new IranianBankCardValidator().isValid("6037997512345670", null));
    }
}
