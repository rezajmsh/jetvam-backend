package ir.jetvam.infra.i18n.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

@ConfigurationProperties("jetvam.i18n")
@Getter
@Setter
public class JetvamI18nProperties {

    private boolean enabled = true;
    private Locale defaultLocale = Locale.forLanguageTag("fa-IR");
    private String tableName = "i18n_message";
    private String cacheName = "i18n-messages";
    private boolean fallbackToLanguage = true;
    private boolean fallbackToDefaultLocale = true;
    private boolean useCodeAsDefaultMessage;

}
