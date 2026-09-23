package ir.jetvam.infra.i18n;

import ir.jetvam.infra.cache.SpringJetvamCache;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the behavior of database message resolver.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class DatabaseMessageResolverTest {

    @Test
    void resolvesFormatsFallsBackAndCachesDatabaseMessages() {
        AtomicInteger reads = new AtomicInteger();
        MessageRepository repository = locale -> {
            reads.incrementAndGet();
            return switch (locale) {
                case "fa-IR" -> Map.of("hello", "سلام {0}");
                case "en-US" -> Map.of("hello", "Hello {0}");
                default -> Map.of();
            };
        };
        DatabaseMessageResolver resolver = resolver(repository);

        assertEquals("سلام جت‌وام", resolver.getMessage("hello", Locale.forLanguageTag("fa-IR"), "جت‌وام"));
        assertEquals("سلام کاربر", resolver.getMessage("hello", Locale.forLanguageTag("fa-IR"), "کاربر"));
        assertEquals(1, reads.get());
        assertEquals("پیش‌فرض", resolver.getMessageOrDefault("missing", "پیش‌فرض", Locale.forLanguageTag("fa-IR")));
        assertThrows(NoSuchMessageException.class,
                () -> resolver.getMessage("missing", Locale.forLanguageTag("fa-IR")));
    }

    @Test
    void invalidationReloadsMessages() {
        AtomicInteger reads = new AtomicInteger();
        DatabaseMessageResolver resolver = resolver(locale -> Map.of("version", "v" + reads.incrementAndGet()));
        Locale locale = Locale.forLanguageTag("fa-IR");

        assertEquals("v1", resolver.getMessage("version", locale));
        resolver.refresh(locale);
        assertEquals("v2", resolver.getMessage("version", locale));
    }

    private static DatabaseMessageResolver resolver(MessageRepository repository) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        return new DatabaseMessageResolver(
                repository,
                new SpringJetvamCache(manager),
                "i18n-messages",
                Locale.forLanguageTag("fa-IR"),
                true,
                true
        );
    }
}
