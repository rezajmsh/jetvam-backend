package ir.jetvam.infra.i18n;

import ir.jetvam.infra.cache.JetvamCache;
import ir.jetvam.common.validation.Preconditions;
import org.springframework.context.NoSuchMessageException;

import java.io.Serial;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves localized messages from the database through the shared cache.
 * Fallback behavior covers locale, language and default values.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class DatabaseMessageResolver implements MessageResolver {

    private final MessageRepository repository;
    private final JetvamCache cache;
    private final String cacheName;
    private final Locale defaultLocale;
    private final boolean fallbackToLanguage;
    private final boolean fallbackToDefaultLocale;
    private final Set<String> loadedLocaleKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public DatabaseMessageResolver(
            MessageRepository repository,
            JetvamCache cache,
            String cacheName,
            Locale defaultLocale,
            boolean fallbackToLanguage,
            boolean fallbackToDefaultLocale
    ) {
        this.repository = Preconditions.requireNonNull(repository, "repository");
        this.cache = Preconditions.requireNonNull(cache, "cache");
        this.cacheName = Preconditions.requireText(cacheName, "cacheName");
        this.defaultLocale = Preconditions.requireNonNull(defaultLocale, "defaultLocale");
        this.fallbackToLanguage = fallbackToLanguage;
        this.fallbackToDefaultLocale = fallbackToDefaultLocale;
    }

    @Override
    public String getMessage(String code, Locale locale, Object... arguments) {
        String pattern = findMessagePattern(code, locale)
                .orElseThrow(() -> new NoSuchMessageException(code, effectiveLocale(locale)));
        return format(pattern, effectiveLocale(locale), arguments);
    }

    @Override
    public String getMessageOrDefault(
            String code,
            String defaultMessage,
            Locale locale,
            Object... arguments
    ) {
        String pattern = findMessagePattern(code, locale).orElse(defaultMessage);
        return format(pattern, effectiveLocale(locale), arguments);
    }

    @Override
    public Optional<String> findMessagePattern(String code, Locale locale) {
        String requiredCode = Preconditions.requireText(code, "code");
        for (Locale candidate : fallbackChain(effectiveLocale(locale))) {
            String pattern = messages(candidate).values().get(requiredCode);
            if (pattern != null) {
                return Optional.of(pattern);
            }
        }
        return Optional.empty();
    }

    @Override
    public void refresh() {
        cache.clear(cacheName);
        loadedLocaleKeys.clear();
    }

    @Override
    public void refresh(Locale locale) {
        for (Locale candidate : fallbackChain(effectiveLocale(locale))) {
            String key = cacheKey(candidate);
            cache.evict(cacheName, key);
            loadedLocaleKeys.remove(key);
        }
    }

    private CachedMessages messages(Locale locale) {
        String key = cacheKey(locale);
        Optional<CachedMessages> cached = cache.get(cacheName, key, CachedMessages.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        CachedMessages loaded = new CachedMessages(repository.findActiveMessages(locale.toLanguageTag()));
        cache.put(cacheName, key, loaded);
        loadedLocaleKeys.add(key);
        return loaded;
    }

    private List<Locale> fallbackChain(Locale requestedLocale) {
        LinkedHashSet<Locale> chain = new LinkedHashSet<>();
        addLocaleAndLanguage(chain, requestedLocale);
        if (fallbackToDefaultLocale) {
            addLocaleAndLanguage(chain, defaultLocale);
        }
        return new ArrayList<>(chain);
    }

    private void addLocaleAndLanguage(Set<Locale> chain, Locale locale) {
        chain.add(locale);
        if (fallbackToLanguage && !locale.getLanguage().isBlank()
                && !locale.toLanguageTag().equals(locale.getLanguage())) {
            chain.add(Locale.forLanguageTag(locale.getLanguage()));
        }
    }

    private Locale effectiveLocale(Locale locale) {
        return locale == null ? defaultLocale : locale;
    }

    private static String cacheKey(Locale locale) {
        return locale.toLanguageTag().toLowerCase(Locale.ROOT);
    }

    private static String format(String pattern, Locale locale, Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return pattern;
        }
        return new MessageFormat(pattern, locale).format(arguments);
    }

    /**
     * Stores an immutable locale-specific message map in the cache.
     * Serialization allows the same value to work with local and Redis providers.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    private record CachedMessages(Map<String, String> values) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private CachedMessages {
            values = Map.copyOf(values);
        }
    }
}
