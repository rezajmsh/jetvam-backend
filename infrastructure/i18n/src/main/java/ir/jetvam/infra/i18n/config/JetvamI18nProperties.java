package ir.jetvam.infra.i18n.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

/**
 * Binds external settings for the jetvam i18n infrastructure.
 * Typed defaults and validation keep application configuration consistent.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@ConfigurationProperties("jetvam.i18n")
@Getter
@Setter
public class JetvamI18nProperties {

    private boolean enabled = true;
    private Locale defaultLocale = Locale.forLanguageTag("fa-IR");
    private String cacheName = "i18n-messages";
    private boolean fallbackToLanguage = true;
    private boolean fallbackToDefaultLocale = true;
    private boolean useCodeAsDefaultMessage;

}
