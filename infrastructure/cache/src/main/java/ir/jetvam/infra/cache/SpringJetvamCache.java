package ir.jetvam.infra.cache;

import ir.jetvam.common.validation.Preconditions;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;

public final class SpringJetvamCache implements JetvamCache {

    private final CacheManager cacheManager;

    public SpringJetvamCache(CacheManager cacheManager) {
        this.cacheManager = Preconditions.requireNonNull(cacheManager, "cacheManager");
    }

    @Override
    public <T> Optional<T> get(String cacheName, Object key, Class<T> valueType) {
        Preconditions.requireNonNull(valueType, "valueType");
        return Optional.ofNullable(requiredCache(cacheName).get(requiredKey(key), valueType));
    }

    @Override
    public void put(String cacheName, Object key, Object value) {
        requiredCache(cacheName).put(requiredKey(key), Preconditions.requireNonNull(value, "value"));
    }

    @Override
    public void evict(String cacheName, Object key) {
        requiredCache(cacheName).evict(requiredKey(key));
    }

    @Override
    public boolean evictIfPresent(String cacheName, Object key) {
        return requiredCache(cacheName).evictIfPresent(requiredKey(key));
    }

    @Override
    public void clear(String cacheName) {
        requiredCache(cacheName).clear();
    }

    private Cache requiredCache(String cacheName) {
        Preconditions.requireText(cacheName, "cacheName");
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalArgumentException("Unknown cache: " + cacheName);
        }
        return cache;
    }

    private static Object requiredKey(Object key) {
        return Preconditions.requireNonNull(key, "cache key");
    }
}
