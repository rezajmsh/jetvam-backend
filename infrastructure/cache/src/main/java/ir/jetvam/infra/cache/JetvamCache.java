package ir.jetvam.infra.cache;

import java.util.Optional;

public interface JetvamCache {

    <T> Optional<T> get(String cacheName, Object key, Class<T> valueType);

    void put(String cacheName, Object key, Object value);

    void evict(String cacheName, Object key);

    boolean evictIfPresent(String cacheName, Object key);

    void clear(String cacheName);
}
