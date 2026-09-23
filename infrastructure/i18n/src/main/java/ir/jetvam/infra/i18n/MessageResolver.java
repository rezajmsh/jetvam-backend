package ir.jetvam.infra.i18n;

import java.util.Locale;
import java.util.Optional;

/**
 * Defines manual localized-message lookup for business and infrastructure code.
 * It complements Spring MessageSource integration.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public interface MessageResolver {

    String getMessage(String code, Locale locale, Object... arguments);

    String getMessageOrDefault(String code, String defaultMessage, Locale locale, Object... arguments);

    Optional<String> findMessagePattern(String code, Locale locale);

    void refresh();

    void refresh(Locale locale);
}
