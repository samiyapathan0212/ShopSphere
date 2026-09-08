package com.shopsphere.backend.service;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Small helper around the Spring Cache {@link CacheManager} abstraction used by the
 * catalog services. It provides the two operations the catalog needs:
 * <ul>
 * <li>{@link #getCached(String, Object, Supplier)}: cache-aside read with a loader;
 *     cache misses populate the cache, cache failures (e.g. Redis unavailable) are
 *     logged and fall through to the database, so the catalog degrades gracefully.</li>
 * <li>{@link #evict(String...)}: best-effort full eviction of the given cache names;
 *     failures are logged and ignored (a stale entry merely expires via its TTL).</li>
 * </ul>
 * Only catalog DTOs ever touch the cache, never authentication/user data. A
 * {@code null} {@link CacheManager} (unit tests without caching) disables caching
 * entirely without changing behavior.
 */
@Component
public class CatalogCacheSupport {

    private static final Logger log = LoggerFactory.getLogger(CatalogCacheSupport.class);

    private final CacheManager cacheManager;

    public CatalogCacheSupport(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Returns the cached value for {@code key} or loads it through {@code loader},
     * caching the result. Cache failures never propagate: they are logged and the
     * loader is used instead, so catalog reads keep working when Redis is temporarily
     * unavailable.
     *
     * @param cacheName cache to use
     * @param key       cache key (must include every input that affects the value)
     * @param loader     supplier loading the value on a miss
     * @param <T>       value type
     * @return the cached or freshly loaded value
     */
    @SuppressWarnings("unchecked")
    public <T> T getCached(String cacheName, Object key, Supplier<T> loader) {
        Cache cache = cacheManager == null ? null : cacheManager.getCache(cacheName);
        if (cache != null) {
            try {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null && wrapper.get() != null) {
                    return (T) wrapper.get();
                }
            } catch (RuntimeException ex) {
                log.warn("Cache read failed for {}:{} - falling back to database", cacheName, key, ex);
            }
        }
        T value = loader.get();
        if (cache != null) {
            try {
                cache.put(key, value);
            } catch (RuntimeException ex) {
                log.warn("Cache write failed for {}:{} - continuing without cache", cacheName, key, ex);
            }
        }
        return value;
    }

    /**
     * Best-effort eviction of the given caches (used after admin mutations).
     * Failures are logged and ignored, so writes never fail because Redis is down.
     */
    public void evict(String... cacheNames) {
        if (cacheManager == null || cacheNames == null) {
            return;
        }
        for (String cacheName : cacheNames) {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            } catch (RuntimeException ex) {
                log.warn("Cache eviction failed for {} - continuing without eviction", cacheName, ex);
            }
        }
    }
}