package ir.jetvam.modules.otp.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that deterministic OTP codes remain restricted to declared development environments.
 * Invalid fixed codes and accidental production activation must fail during application startup.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
class OtpConfigurationValidatorTest {

    @Test
    void acceptsDevelopmentBypassInLocalEnvironment() {
        OtpProperties properties = validProperties();
        properties.getDevelopmentBypass().setEnabled(true);

        assertThatCode(() -> new OtpConfigurationValidator(properties).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDevelopmentBypassInProduction() {
        OtpProperties properties = validProperties();
        properties.getDevelopmentBypass().setEnabled(true);
        properties.getDevelopmentBypass().setEnvironment("production");

        assertThatThrownBy(() -> new OtpConfigurationValidator(properties).afterPropertiesSet())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OTP development bypass cannot be enabled in environment production");
    }

    @Test
    void rejectsFixedCodeWithUnexpectedLength() {
        OtpProperties properties = validProperties();
        properties.getDevelopmentBypass().setEnabled(true);
        properties.getDevelopmentBypass().setCode("1234");

        assertThatThrownBy(() -> new OtpConfigurationValidator(properties).afterPropertiesSet())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OTP development bypass code must contain exactly 6 digits");
    }

    private static OtpProperties validProperties() {
        OtpProperties properties = new OtpProperties();
        properties.setHmacSecret("0123456789abcdef0123456789abcdef");
        return properties;
    }
}
