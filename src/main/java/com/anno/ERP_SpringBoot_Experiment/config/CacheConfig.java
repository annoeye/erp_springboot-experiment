package com.anno.ERP_SpringBoot_Experiment.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Cấu hình Spring Cache với Caffeine (in-memory cache).
 * Cache cho các đối tượng đọc nhiều, ghi ít (chỉ dùng Entity Detail Cache):
 * - productDetails: chi tiết sản phẩm (TTL 10 phút, max 1000 entries)
 * - categoryDetails: chi tiết danh mục (TTL 30 phút, max 200 entries)
 * - attributes: chi tiết biến thể/thuộc tính (TTL 5 phút, max 2000 entries)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Constants định nghĩa tên các thùng chứa chi tiết thực thể
    public static final String CACHE_PRODUCT_DETAILS = "productDetails";
    public static final String CACHE_CATEGORY_DETAILS = "categoryDetails";
    public static final String CACHE_ATTRIBUTES = "attributes";

    private record CacheSpec(String name, Duration ttl, long maxSize) {}

    // Danh sách cấu hình các thùng chứa chi tiết
    private static final List<CacheSpec> CACHE_SPECS = List.of(
            new CacheSpec(CACHE_PRODUCT_DETAILS, Duration.ofMinutes(10), 1000),
            new CacheSpec(CACHE_CATEGORY_DETAILS, Duration.ofMinutes(30), 200),
            new CacheSpec(CACHE_ATTRIBUTES, Duration.ofMinutes(5), 2000)
    );

    @Bean
    public CacheManager cacheManager() {
        var cacheManager = new CaffeineCacheManager();
        
        CACHE_SPECS.forEach(spec -> cacheManager.registerCustomCache(
                spec.name(),
                Caffeine.newBuilder()
                        .expireAfterWrite(spec.ttl())
                        .maximumSize(spec.maxSize())
                        .recordStats()
                        .build()
        ));
        
        return cacheManager;
    }
}