package com.anno.ERP_SpringBoot_Experiment.caffeine_cache;

import com.anno.ERP_SpringBoot_Experiment.caffeine_cache.CacheConfig;
import com.anno.ERP_SpringBoot_Experiment.mapper.ProductMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.caffeine_cache.CacheSyncService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for CacheSyncService – background cache synchronization.
 *
 * CacheSyncService maintains a set of "dirty" product IDs that need cache refresh.
 * A scheduled task (every 5 min) processes these IDs and updates the Caffeine cache.
 *
 * This test verifies:
 * - Dirty marking mechanism
 * - Cache eviction for deleted products
 * - Cache refresh for existing products
 * - Graceful handling of missing cache
 */
@DisplayName("CacheSyncService – Background cache sync")
@ExtendWith(MockitoExtension.class)
class CacheSyncServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;

    private CacheManager cacheManager;
    private CacheSyncService cacheSyncService;

    private Product product;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        var config = new CacheConfig();
        cacheManager = config.cacheManager();

        cacheSyncService = new CacheSyncService(
                productRepository, productMapper, cacheManager
        );

        product = Product.builder().id(1L).name("Test Product")
                .status(ActiveStatus.ACTIVE).build();

        productDto = new ProductDto();
        productDto.setId(1L);
        productDto.setName("Test Product");
    }

    @Test
    @DisplayName("markProductDirty should add ID to dirty set")
    void markProductDirty_AddsIdToDirtySet() {
        // This is verified indirectly: after marking, sync should process it
        cacheSyncService.markProductDirty(1L);
        cacheSyncService.markProductDirty(2L);
        cacheSyncService.markProductDirty(1L); // duplicate

        // Act: trigger sync
        when(productRepository.findByIdWithDetails(1L))
                .thenReturn(Optional.of(product));
        when(productRepository.findByIdWithDetails(2L))
                .thenReturn(Optional.of(product));
        when(productMapper.toDto(any(Product.class))).thenReturn(productDto);

        cacheSyncService.syncDirtyProductCaches();

        // Assert: both unique IDs were processed
        verify(productRepository, times(1)).findByIdWithDetails(1L);
        verify(productRepository, times(1)).findByIdWithDetails(2L);
    }

    @Test
    @DisplayName("syncDirtyProductCaches should update cache for existing products")
    void sync_UpdatesCacheForExistingProducts() {
        // Arrange: mark dirty
        cacheSyncService.markProductDirty(1L);

        // Act: sync
        when(productRepository.findByIdWithDetails(1L))
                .thenReturn(Optional.of(product));
        when(productMapper.toDto(any(Product.class))).thenReturn(productDto);

        cacheSyncService.syncDirtyProductCaches();

        // Assert: cache should now contain the product
        var cache = cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAILS);
        assertNotNull(cache);
        var cached = cache.get(1L, ProductDto.class);
        assertNotNull(cached);
        assertEquals("Test Product", cached.getName());
    }

    @Test
    @DisplayName("syncDirtyProductCaches should evict cache for deleted products")
    void sync_EvictsCacheForDeletedProducts() {
        // Arrange: pre-populate cache with product that will be "deleted"
        var cache = cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAILS);
        assertNotNull(cache);
        cache.put(1L, productDto);
        assertNotNull(cache.get(1L, ProductDto.class));

        // Mark dirty
        cacheSyncService.markProductDirty(1L);

        // Act: sync – product no longer exists in DB
        when(productRepository.findByIdWithDetails(1L))
                .thenReturn(Optional.empty());

        cacheSyncService.syncDirtyProductCaches();

        // Assert: cache entry should be evicted
        assertNull(cache.get(1L, ProductDto.class));
    }

    @Test
    @DisplayName("syncDirtyProductCaches should do nothing when dirty set is empty")
    void sync_WhenDirtySetEmpty_DoesNothing() {
        cacheSyncService.syncDirtyProductCaches();

        verify(productRepository, never()).findByIdWithDetails(anyLong());
        verify(productMapper, never()).toDto(any(Product.class));
    }

    @Test
    @DisplayName("markProductDirty followed by sync should clear dirty set")
    void sync_ClearsDirtySetAfterProcessing() {
        // Arrange
        cacheSyncService.markProductDirty(1L);
        when(productRepository.findByIdWithDetails(1L))
                .thenReturn(Optional.of(product));
        when(productMapper.toDto(any(Product.class))).thenReturn(productDto);

        // Act: first sync
        cacheSyncService.syncDirtyProductCaches();
        verify(productRepository, times(1)).findByIdWithDetails(1L);

        // Act: second sync – should not process again
        cacheSyncService.syncDirtyProductCaches();
        verify(productRepository, times(1)).findByIdWithDetails(1L);
    }

    @Test
    @DisplayName("Multiple markProductDirty calls with same ID should sync only once")
    void duplicateMarks_ShouldSyncOnlyOnce() {
        // Arrange: mark same ID three times
        cacheSyncService.markProductDirty(1L);
        cacheSyncService.markProductDirty(1L);
        cacheSyncService.markProductDirty(1L);

        when(productRepository.findByIdWithDetails(1L))
                .thenReturn(Optional.of(product));
        when(productMapper.toDto(any(Product.class))).thenReturn(productDto);

        // Act
        cacheSyncService.syncDirtyProductCaches();

        // Assert: only one DB call
        verify(productRepository, times(1)).findByIdWithDetails(1L);
    }

    @Test
    @DisplayName("Updated cache entry should be retrievable via getProductById/Ids")
    void syncedCache_ShouldBeUsableByServices() {
        // Simulate full flow: mark → sync → retrieve from cache

        // Step 1: Put initial data in cache
        var cache = cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAILS);
        assertNotNull(cache);
        cache.put(1L, productDto);

        // Step 2: CacheSyncService updates it with new data
        ProductDto updatedDto = new ProductDto();
        updatedDto.setId(1L);
        updatedDto.setName("Updated Product Name");

        cacheSyncService.markProductDirty(1L);

        Product updatedProduct = Product.builder().id(1L).name("Updated Product Name")
                .status(ActiveStatus.ACTIVE).build();

        when(productRepository.findByIdWithDetails(1L))
                .thenReturn(Optional.of(updatedProduct));
        when(productMapper.toDto(any(Product.class))).thenReturn(updatedDto);

        cacheSyncService.syncDirtyProductCaches();

        // Step 3: Verify cache contains updated data
        ProductDto cached = cache.get(1L, ProductDto.class);
        assertNotNull(cached);
        assertEquals("Updated Product Name", cached.getName());
    }
}
