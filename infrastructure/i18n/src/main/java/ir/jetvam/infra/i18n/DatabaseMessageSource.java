package ir.jetvam.infra.i18n;

import ir.jetvam.common.validation.Preconditions;
import org.springframework.context.support.AbstractMessageSource;

import java.text.MessageFormat;
import java.util.Locale;

public final class DatabaseMessageSource extends AbstractMessageSource {

    private final MessageResolver resolver;

    public DatabaseMessageSource(MessageResolver resolver, boolean useCodeAsDefaultMessage) {
        this.resolver = Preconditions.requireNonNull(resolver, "resolver");
        setUseCodeAsDefaultMessage(useCodeAsDefaultMessage);
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        return resolver.findMessagePattern(code, locale)
                .map(pattern -> createMessageFormat(pattern, locale))
                .orElse(null);
    }
}
