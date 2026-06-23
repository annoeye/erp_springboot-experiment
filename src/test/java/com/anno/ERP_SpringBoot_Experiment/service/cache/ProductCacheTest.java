package com.anno.ERP_SpringBoot_Experiment.service.cache;

import com.anno.ERP_SpringBoot_Experiment.config.CacheConfig;
import com.anno.ERP_SpringBoot_Experiment.mapper.ProductMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.CacheSyncService;
import com.anno.ERP_SpringBoot_Experiment.service.MinioService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.Helper;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.ProductService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for ProductService caching.
 *
 * ProductService uses two caching strategies:
 * 1. @Cacheable("productDetails") on getProductById() – requires Spring AOP (not tested here with Mockito)
 * 2. CacheUtils.getAll() in getProductsByIds() – tested directly
 * 3. Base getProductById() logic – tested directly
 * 4. CacheSyncService.markProductDirty() on update – tested
 * 5. Cache consistency between getProductsByIds and CacheManager
 *
 * Note: @Cacheable/@CacheEvict annotations require Spring AOP proxy.
 * This test focuses on the CacheUtils-based batch caching which works directly
 * with Caffeine and does not require AOP.
 */
@DisplayName("Product Caching – CacheUtils + direct cache + CacheSyncService")
@ExtendWith(MockitoExtension.class)
class ProductCacheTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private Helper helper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private CacheSyncService cacheSyncService;
    @Mock
    private MinioService minioService;
    @Mock
    private jakarta.persistence.EntityManager entityManager;

    private CacheManager cacheManager;
    private ProductService productService;

    private Product product1;
    private Product product2;
    private ProductDto dto1;
    private ProductDto dto2;

    @BeforeEach
    void setUp() {
        var config = new CacheConfig();
        cacheManager = config.cacheManager();

        productService = new ProductService(
                productRepository, categoryRepository, securityUtil, helper,
                minioService, productMapper, cacheSyncService, cacheManager, entityManager
        );

        product1 = Product.builder().id(10L).name("Laptop")
                .status(ActiveStatus.ACTIVE).build();
        product2 = Product.builder().id(20L).name("Phone")
                .status(ActiveStatus.ACTIVE).build();

        dto1 = new ProductDto();
        dto1.setId(10L);
        dto1.setName("Laptop");
        var skuInfo1 = new com.anno.ERP_SpringBoot_Experiment.service.dto.SkuInfoDto();
        skuInfo1.setSku("prd-LAP-010");
        dto1.setSkuInfo(skuInfo1);

        dto2 = new ProductDto();
        dto2.setId(20L);
        dto2.setName("Phone");
        var skuInfo2 = new com.anno.ERP_SpringBoot_Experiment.service.dto.SkuInfoDto();
        skuInfo2.setSku("prd-PHN-020");
        dto2.setSkuInfo(skuInfo2);
    }

    // ==================== Direct getProductById (basic service logic) ====================

    @Test
    @DisplayName("getProductById loads from DB when not cached (no AOP for @Cacheable)")
    void getProductById_LoadsFromDb() {
        // In Mockito tests, @Cacheable is not active, so every call goes to DB
        // This test verifies the base service logic
        when(productRepository.findByIdWithDetails(10L))
                .thenReturn(Optional.of(product1));
        when(productMapper.toDto(product1)).thenReturn(dto1);

        ProductDto result = productService.getProductById(10L);

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        verify(productRepository, times(1)).findByIdWithDetails(10L);
        verify(productMapper, times(1)).toDto(product1);
    }

    // ==================== CacheUtils: getProductsByIds() ====================

    @Test
    @DisplayName("getProductsByIds: mixed cache hit/miss batch loading")
    void getProductsByIds_MixedCacheHitAndMiss() {
        // Arrange: pre-cache product1
        var cache = cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAILS);
        assertNotNull(cache);
        cache.put(10L, dto1);

        // product2 must be loaded from DB
        when(productRepository.findAllById(List.of(20L)))
                .thenReturn(List.of(product2));
        when(productMapper.toDto(product2)).thenReturn(dto2);

        // Act
        Response<List<ProductDto>> response =
                productService.getProductsByIds(List.of(10L, 20L));

        // Assert
        assertEquals(2, response.getData().size());
        assertEquals("Laptop", response.getData().get(0).getName());
        assertEquals("Phone", response.getData().get(1).getName());
        verify(productRepository, times(1)).findAllById(List.of(20L));
        verify(productMapper, times(1)).toDto(product2);
    }

    @Test
    @DisplayName("getProductsByIds: empty/null IDs return empty without DB call")
    void getProductsByIds_EmptyOrNullIds() {
        Response<List<ProductDto>> nullResult =
                productService.getProductsByIds(null);
        assertTrue(nullResult.getData().isEmpty());
        verify(productRepository, never()).findAllById(anyList());

        Response<List<ProductDto>> emptyResult =
                productService.getProductsByIds(List.of());
        assertTrue(emptyResult.getData().isEmpty());
        verify(productRepository, never()).findAllById(anyList());
    }

    @Test
    @DisplayName("getProductsByIds: first call misses cache → loads from DB → subsequent calls hit cache")
    void getProductsByIds_CacheMissThenHit() {
        // Arrange
        when(productRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(product1));
        when(productMapper.toDto(product1)).thenReturn(dto1);

        // Act: first call
        Response<List<ProductDto>> firstResponse =
                productService.getProductsByIds(List.of(10L));
        assertEquals(1, firstResponse.getData().size());

        // Act: second call (should hit cache)
        Response<List<ProductDto>> secondResponse =
                productService.getProductsByIds(List.of(10L));
        assertEquals(1, secondResponse.getData().size());

        // Assert: only one DB call
        verify(productRepository, times(1)).findAllById(anyList());
        verify(productMapper, times(1)).toDto(any(Product.class));
    }

    @Test
    @DisplayName("getProductsByIds: different IDs cached independently")
    void getProductsByIds_DifferentIdsCachedIndependently() {
        // Arrange
        when(productRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(product1));
        when(productRepository.findAllById(List.of(20L)))
                .thenReturn(List.of(product2));
        when(productMapper.toDto(product1)).thenReturn(dto1);
        when(productMapper.toDto(product2)).thenReturn(dto2);

        // First calls
        productService.getProductsByIds(List.of(10L));
        productService.getProductsByIds(List.of(20L));

        // Second calls (should hit cache)
        Response<List<ProductDto>> r1 = productService.getProductsByIds(List.of(10L));
        Response<List<ProductDto>> r2 = productService.getProductsByIds(List.of(20L));

        assertEquals("Laptop", r1.getData().get(0).getName());
        assertEquals("Phone", r2.getData().get(0).getName());
        verify(productRepository, times(1)).findAllById(List.of(10L));
        verify(productRepository, times(1)).findAllById(List.of(20L));
    }

    // ==================== Cache Consistency ====================

    @Test
    @DisplayName("getProductById loads from DB each time (no AOP), but getProductsByIds uses CacheUtils")
    void getProductById_And_getProductsByIds_UseSameCacheRegion() {
        // getProductById returns fresh data each time (no AOP proxy active)
        when(productRepository.findByIdWithDetails(10L))
                .thenReturn(Optional.of(product1));
        when(productMapper.toDto(product1)).thenReturn(dto1);

        ProductDto byIdResult = productService.getProductById(10L);
        assertEquals("Laptop", byIdResult.getName());

        // getProductsByIds should load from DB since cache is empty
        when(productRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(product1));

        Response<List<ProductDto>> byIdsResult =
                productService.getProductsByIds(List.of(10L));
        assertEquals(1, byIdsResult.getData().size());
    }

    // ==================== updateProduct marks dirty ====================

    @Test
    @DisplayName("updateProduct marks dirty via CacheSyncService")
    void updateProduct_MarksDirty() {
        var request = new com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProductRequest();
        request.setId("10");
        request.setName("Updated Laptop");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product1));

        productService.updateProduct(request);

        verify(cacheSyncService).markProductDirty(10L);
    }

    // ==================== getProductsBySkus() ====================

    @Test
    @DisplayName("getProductsBySkus resolves SKUs to IDs and caches results")
    void getProductsBySkus_ResolvesSkusAndCaches() {
        // Arrange
        List<Object[]> rows = List.of(
                new Object[]{10L, "prd-LAP-010"},
                new Object[]{20L, "prd-PHN-020"}
        );
        when(productRepository.findIdsAndSkusBySkus(anyList()))
                .thenReturn(rows);
        when(productRepository.findAllById(anyList()))
                .thenReturn(List.of(product1, product2));
        when(productMapper.toDto(product1)).thenReturn(dto1);
        when(productMapper.toDto(product2)).thenReturn(dto2);

        // Act
        Response<List<ProductDto>> response = productService
                .getProductsBySkus(List.of("prd-LAP-010", "prd-PHN-020"));

        assertEquals(2, response.getData().size());
        assertEquals("Laptop", response.getData().get(0).getName());

        // Second call should hit cache
        Response<List<ProductDto>> secondResponse = productService
                .getProductsBySkus(List.of("prd-LAP-010"));
        assertEquals(1, secondResponse.getData().size());
        verify(productRepository, times(1)).findAllById(anyList());
    }

    // ==================== Direct cache operations ====================

    @Test
    @DisplayName("Direct cache put/get/evict in productDetails cache works")
    void directCacheOperations_WorkCorrectly() {
        var cache = cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAILS);
        assertNotNull(cache);

        cache.put(100L, dto1);
        ProductDto cached = cache.get(100L, ProductDto.class);
        assertNotNull(cached);
        assertEquals("Laptop", cached.getName());

        cache.evict(100L);
        assertNull(cache.get(100L, ProductDto.class));
    }

    @Test
    @DisplayName("getProductsByIds stores loaded data in cache for reuse")
    void getProductsByIds_StoresLoadedDataInCache() {
        // Arrange
        when(productRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(product1));
        when(productMapper.toDto(product1)).thenReturn(dto1);

        // Load from DB and populate cache
        productService.getProductsByIds(List.of(10L));

        // Verify cache now holds the data
        var cache = cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAILS);
        assertNotNull(cache);
        ProductDto cached = cache.get(10L, ProductDto.class);
        assertNotNull(cached);
        assertEquals("Laptop", cached.getName());
    }
}
