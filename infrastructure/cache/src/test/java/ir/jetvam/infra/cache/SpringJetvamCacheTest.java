package ir.jetvam.infra.cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies the behavior of spring jetvam cache.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class SpringJetvamCacheTest {

    @Test
    void supportsManualCacheOperations() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        SpringJetvamCache cache = new SpringJetvamCache(manager);

        cache.put("messages", "fa:greeting", "سلام");
        assertEquals("سلام", cache.get("messages", "fa:greeting", String.class).orElseThrow());

        cache.evict("messages", "fa:greeting");
        assertFalse(cache.get("messages", "fa:greeting", String.class).isPresent());
    }
}
