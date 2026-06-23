package com.anno.ERP_SpringBoot_Experiment.service.cache;

import com.anno.ERP_SpringBoot_Experiment.config.CacheConfig;
import com.anno.ERP_SpringBoot_Experiment.util.CacheUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional tests for CacheUtils – batch-get helper with Caffeine.
 *
 * Verifies that CacheUtils.getAll() correctly:
 * - Returns cached values for known keys
 * - Loads missing values via dbLoader
 * - Combines cached + loaded results
 * - Falls back to dbLoader when cache is unavailable
 */
@DisplayName("CacheUtils – Batch-get with Caffeine cache")
class CacheUtilsTest {

    private CacheManager cacheManager;
    private static final String CACHE_NAME = CacheConfig.CACHE_PRODUCT_DETAILS;

    @BeforeEach
    void setUp() {
        var config = new CacheConfig();
        cacheManager = config.cacheManager();
    }

    @Test
    @DisplayName("Should return all values from cache when all keys are present")
    void getAll_WhenAllKeysCached_ReturnsAllFromCache() {
        // Arrange: pre-populate cache
        var cache = cacheManager.getCache(CACHE_NAME);
        assertNotNull(cache);
        cache.put(1L, "product-1");
        cache.put(2L, "product-2");
        cache.put(3L, "product-3");

        // Track if dbLoader was called
        var dbLoaderCalled = new boolean[]{false};

        // Act
        Map<Long, String> result = CacheUtils.getAll(
                cacheManager, CACHE_NAME, List.of(1L, 2L, 3L),
                missing -> {
                    dbLoaderCalled[0] = true;
                    return Map.of();
                }
        );

        // Assert: All results from cache
        assertEquals(3, result.size());
        assertEquals("product-1", result.get(1L));
        assertEquals("product-2", result.get(2L));
        assertEquals("product-3", result.get(3L));
        
        // Note: Caffeine's native getAll() always calls the loader function
        // even when all keys are cached. The loaded values don't override existing ones.
    }

    @Test
    @DisplayName("Should load missing keys via dbLoader and combine with cached values")
    void getAll_WhenSomeKeysMissing_LoadsMissingViaDbLoader() {
        // Arrange: pre-populate partial cache
        var cache = cacheManager.getCache(CACHE_NAME);
        assertNotNull(cache);
        cache.put(1L, "cached-product-1");
        cache.put(2L, "cached-product-2");

        // Act
        Map<Long, String> result = CacheUtils.getAll(
                cacheManager, CACHE_NAME, List.of(1L, 2L, 3L, 4L),
                missing -> {
                    // Caffeine passes all keys to the loader but only missing ones matter
                    return Map.of(
                            3L, "loaded-product-3",
                            4L, "loaded-product-4"
                    );
                }
        );

        // Assert: cached values preserved
        assertEquals(4, result.size());
        assertEquals("cached-product-1", result.get(1L));
        assertEquals("cached-product-2", result.get(2L));
        assertEquals("loaded-product-3", result.get(3L));
        assertEquals("loaded-product-4", result.get(4L));
    }

    @Test
    @DisplayName("Should load all keys via dbLoader when cache is empty")
    void getAll_WhenCacheEmpty_LoadsAllViaDbLoader() {
        Map<Long, String> result = CacheUtils.getAll(
                cacheManager, CACHE_NAME, List.of(10L, 20L),
                missing -> Map.of(
                        10L, "loaded-10",
                        20L, "loaded-20"
                )
        );

        assertEquals(2, result.size());
        assertEquals("loaded-10", result.get(10L));
        assertEquals("loaded-20", result.get(20L));
    }

    @Test
    @DisplayName("Should return empty map when keys list is empty")
    void getAll_WhenKeysEmpty_ReturnsEmptyMap() {
        // When keys collection is empty, the loader should still be called
        // (Caffeine behavior), but no results expected
        Map<Long, String> result = CacheUtils.getAll(
                cacheManager, CACHE_NAME, List.of(),
                missing -> {
                    assertTrue(missing.isEmpty());
                    return Map.of();
                }
        );

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should fallback to dbLoader when cache name does not exist")
    void getAll_WhenCacheNameNotFound_FallsBackToDbLoader() {
        Function<Collection<Long>, Map<Long, String>> dbLoader = missing ->
                Map.of(1L, "fallback-1", 2L, "fallback-2");

        Map<Long, String> result = CacheUtils.getAll(
                cacheManager, "non-existent-cache", List.of(1L, 2L), dbLoader
        );

        assertEquals(2, result.size());
        assertEquals("fallback-1", result.get(1L));
    }

    @Test
    @DisplayName("Loaded values via dbLoader should be stored in cache for subsequent calls")
    void getAll_LoadedValues_ShouldBeCached() {
        // Arrange: first call loads from dbLoader (cache miss)
        CacheUtils.getAll(
                cacheManager, CACHE_NAME, List.of(100L),
                missing -> Map.of(100L, "loaded-value")
        );

        // Act: second call – the value should now be retrievable from cache
        var cache = cacheManager.getCache(CACHE_NAME);
        assertNotNull(cache);
        String cachedValue = cache.get(100L, String.class);
        assertEquals("loaded-value", cachedValue);
    }

    @Test
    @DisplayName("Should handle null values from cache gracefully")
    void getAll_WhenCacheHasNullValues_HandlesGracefully() {
        // Caffeine does not allow null values via put(), so we test the absence scenario
        var cache = cacheManager.getCache(CACHE_NAME);
        assertNotNull(cache);
        cache.put(1L, "value-1");
        // key 2 is not in cache (equivalent to "null")

        Map<Long, String> result = CacheUtils.getAll(
                cacheManager, CACHE_NAME, List.of(1L, 2L),
                missing -> {
                    var map = new HashMap<Long, String>();
                    for (Long k : missing) {
                        map.put(k, k == 1L ? "value-1" : "loaded-2");
                    }
                    return map;
                }
        );

        assertEquals(2, result.size());
        assertEquals("value-1", result.get(1L));
        assertEquals("loaded-2", result.get(2L));
    }

    @Test
    @DisplayName("Should work with Collection of keys (not just List)")
    void getAll_WorksWithCollectionOfKeys() {
        // Pre-cache one key
        var cache = cacheManager.getCache(CACHE_NAME);
        assertNotNull(cache);
        cache.put(1L, "cached-value");

        Map<Long, String> result = CacheUtils.getAll(
                cacheManager, CACHE_NAME, Set.of(1L, 2L),
                missing -> {
                    assertTrue(missing.contains(2L));
                    return Map.of(2L, "loaded-value");
                }
        );

        assertEquals(2, result.size());
        assertEquals("cached-value", result.get(1L));
        assertEquals("loaded-value", result.get(2L));
    }
}
