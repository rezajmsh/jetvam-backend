package ir.jetvam.infra.cache;

import java.util.Optional;

/**
 * Defines provider-neutral cache operations used by Jetvam modules.
 * Implementations can use local or remote cache providers transparently.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public interface JetvamCache {

    <T> Optional<T> get(String cacheName, Object key, Class<T> valueType);

    void put(String cacheName, Object key, Object value);

    void evict(String cacheName, Object key);

    boolean evictIfPresent(String cacheName, Object key);

    void clear(String cacheName);
}
