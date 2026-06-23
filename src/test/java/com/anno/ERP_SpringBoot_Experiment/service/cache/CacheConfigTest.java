package com.anno.ERP_SpringBoot_Experiment.service.cache;

import com.anno.ERP_SpringBoot_Experiment.config.CacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional tests for CacheConfig – Caffeine Cache configuration.
 *
 * Verifies that the cache manager is properly created with the expected
 * cache regions and specs for productDetails, categoryDetails, and attributes.
 */
@DisplayName("CacheConfig – Caffeine cache configuration")
class CacheConfigTest {

    @Test
    @DisplayName("CacheManager bean should be created with all three cache regions")
    void cacheManager_ShouldHaveAllCacheRegions() {
        var config = new CacheConfig();
        CacheManager cacheManager = config.cacheManager();

        assertNotNull(cacheManager, "CacheManager must not be null");
        assertInstanceOf(CaffeineCacheManager.class, cacheManager,
                "CacheManager must be CaffeineCacheManager");

        // Verify all three cache regions exist
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAILS),
                "productDetails cache must exist");
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_CATEGORY_DETAILS),
                "categoryDetails cache must exist");
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_ATTRIBUTES),
                "attributes cache must exist");
    }

    @Test
    @DisplayName("Cache names constants should match expected values")
    void cacheNameConstants_ShouldBeAsExpected() {
        assertEquals("productDetails", CacheConfig.CACHE_PRODUCT_DETAILS);
        assertEquals("categoryDetails", CacheConfig.CACHE_CATEGORY_DETAILS);
        assertEquals("attributes", CacheConfig.CACHE_ATTRIBUTES);
    }

    @Test
    @DisplayName("Cache should store and retrieve values correctly")
    void cache_ShouldStoreAndRetrieveValues() {
        var config = new CacheConfig();
        CacheManager cacheManager = config.cacheManager();

        var cache = cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAILS);
        assertNotNull(cache);

        // Store value
        cache.put(1L, "test-product-value");
        
        // Retrieve value
        var retrieved = cache.get(1L, String.class);
        assertEquals("test-product-value", retrieved,
                "Cached value should match stored value");
    }

    @Test
    @DisplayName("Cache should evict values correctly")
    void cache_ShouldEvictValues() {
        var config = new CacheConfig();
        CacheManager cacheManager = config.cacheManager();

        var cache = cacheManager.getCache(CacheConfig.CACHE_CATEGORY_DETAILS);
        assertNotNull(cache);

        // Store and verify
        cache.put(10L, "category-data");
        assertEquals("category-data", cache.get(10L, String.class));

        // Evict and verify null
        cache.evict(10L);
        assertNull(cache.get(10L, String.class),
                "Evicted key should return null via get()");
    }

    @Test
    @DisplayName("Different caches should be isolated")
    void caches_ShouldBeIsolated() {
        var config = new CacheConfig();
        CacheManager cacheManager = config.cacheManager();

        var productCache = cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAILS);
        var attrCache = cacheManager.getCache(CacheConfig.CACHE_ATTRIBUTES);
        assertNotNull(productCache);
        assertNotNull(attrCache);

        productCache.put(1L, "product-data");
        attrCache.put(1L, "attribute-data");

        assertEquals("product-data", productCache.get(1L, String.class));
        assertEquals("attribute-data", attrCache.get(1L, String.class));
    }

    @Test
    @DisplayName("Cache should maintain stats (recordStats enabled)")
    void cache_ShouldRecordStats() {
        var config = new CacheConfig();
        CacheManager cacheManager = config.cacheManager();

        var cache = cacheManager.getCache(CacheConfig.CACHE_ATTRIBUTES);
        assertNotNull(cache);

        var nativeCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();
        
        // Initial stats
        var stats = nativeCache.stats();
        assertEquals(0, stats.hitCount(), "Initial hit count should be 0");
        assertEquals(0, stats.missCount(), "Initial miss count should be 0");

        // Miss then hit
        assertNull(cache.get("missing-key"));
        cache.put("existing-key", "value");
        assertNotNull(cache.get("existing-key", String.class));

        stats = nativeCache.stats();
        assertEquals(1, stats.hitCount(), "Should have 1 hit");
        assertEquals(1, stats.missCount(), "Should have 1 miss");
    }
}
