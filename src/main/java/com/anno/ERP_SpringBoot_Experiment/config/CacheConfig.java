package com.anno.ERP_SpringBoot_Experiment.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cấu hình Spring Cache với Caffeine (in-memory cache).
 * Cache cho các đối tượng đọc nhiều, ghi ít:
 * - products: thông tin sản phẩm (TTL 10 phút, max 1000 entries)
 * - categories: danh mục (TTL 30 phút, max 100 entries)
 * - attributes: biến thể SKU (TTL 5 phút, max 2000 entries)
 *
 * @en Caffeine cache configuration for hot objects
 */
@Configuration
public class CacheConfig {

    /**
     * CacheManager với Caffeine làm backend.
     * Mỗi cache region có cấu hình riêng.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Product cache: 10 phút, tối đa 1000 sản phẩm (dùng cho search cache)
        cacheManager.registerCustomCache("products",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .recordStats()
                        .build());

        // Product detail cache: 10 phút, tối đa 1000 sản phẩm (dùng cho lazy load + background sync)
        cacheManager.registerCustomCache("productDetails",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .recordStats()
                        .build());

        // Category cache: 30 phút, tối đa 200 danh mục (dùng cho search cache)
        cacheManager.registerCustomCache("categories",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .recordStats()
                        .build());

        // Category detail cache: 30 phút, tối đa 200 danh mục (dùng cho lazy load)
        cacheManager.registerCustomCache("categoryDetails",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .recordStats()
                        .build());

        // Attributes/SKU cache: 5 phút, tối đa 2000 entries
        cacheManager.registerCustomCache("attributes",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(2000)
                        .recordStats()
                        .build());

        // User profile cache: 15 phút, tối đa 500 users
        cacheManager.registerCustomCache("users",
                Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build());

        return cacheManager;
    }
}
