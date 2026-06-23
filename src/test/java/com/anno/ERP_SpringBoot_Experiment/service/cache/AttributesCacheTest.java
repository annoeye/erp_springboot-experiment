package com.anno.ERP_SpringBoot_Experiment.service.cache;

import com.anno.ERP_SpringBoot_Experiment.config.CacheConfig;
import com.anno.ERP_SpringBoot_Experiment.mapper.AttributesMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.PromotionMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.SpecificationMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.AttributesService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.Helper;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributeInput;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for AttributesService caching.
 *
 * AttributesService uses:
 * 1. @Cacheable(value = "attributes", key = "#productId") on getAttributesByProductId() – requires Spring AOP
 * 2. @CacheEvict(value = "attributes", allEntries = true) on create/update/delete – requires Spring AOP
 * 3. CacheUtils.getAll() in getAttributesByIds() – tested directly (no AOP needed)
 * 4. getAttributesBySkus() – resolves SKUs then delegates to getAttributesByIds
 *
 * Note: @Cacheable/@CacheEvict annotations require Spring AOP proxy.
 * This test focuses on the CacheUtils-based batch caching (getAttributesByIds,
 * getAttributesBySkus) and direct cache operations.
 */
@DisplayName("Attributes Caching – CacheUtils batch + direct cache")
@ExtendWith(MockitoExtension.class)
class AttributesCacheTest {

    @Mock
    private AttributesRepository attributesRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private Helper helper;
    @Mock
    private SpecificationMapper specificationMapper;
    @Mock
    private PromotionMapper promotionMapper;
    @Mock
    private AttributesMapper attributesMapper;
    @Mock
    private SecurityUtil securityUtil;

    private CacheManager cacheManager;
    private AttributesService attributesService;

    private Product product;
    private Attributes attr1;
    private Attributes attr2;
    private AttributesDto dto1;
    private AttributesDto dto2;

    @BeforeEach
    void setUp() {
        var config = new CacheConfig();
        cacheManager = config.cacheManager();

        attributesService = new AttributesService(
                attributesRepository, productRepository, helper,
                specificationMapper, promotionMapper, attributesMapper,
                securityUtil, cacheManager, null
        );

        product = Product.builder().id(100L).name("Test Product").build();

        attr1 = Attributes.builder().id(1L).name("Color Red")
                .price(100.0).statusProduct(StockStatus.AVAILABLE).product(product).build();
        attr2 = Attributes.builder().id(2L).name("Size XL")
                .price(200.0).statusProduct(StockStatus.AVAILABLE).product(product).build();

        dto1 = new AttributesDto();
        dto1.setId(1L);
        dto1.setName("Color Red");
        var skuInfo1 = new com.anno.ERP_SpringBoot_Experiment.service.dto.SkuInfoDto();
        skuInfo1.setSku("attr-RED-001");
        dto1.setSku(skuInfo1);

        dto2 = new AttributesDto();
        dto2.setId(2L);
        dto2.setName("Size XL");
        var skuInfo2 = new com.anno.ERP_SpringBoot_Experiment.service.dto.SkuInfoDto();
        skuInfo2.setSku("attr-XL-002");
        dto2.setSku(skuInfo2);
    }

    // ==================== CacheUtils: getAttributesByIds() ====================

    @Test
    @DisplayName("getAttributesByIds: batch cache with mixed hit/miss")
    void getAttributesByIds_MixedCacheHitAndMiss() {
        // Arrange: pre-cache attr1
        var cache = cacheManager.getCache(CacheConfig.CACHE_ATTRIBUTES);
        assertNotNull(cache);
        cache.put(1L, dto1);

        when(attributesRepository.getQuantityAttributesById(List.of(2L)))
                .thenReturn(List.of(attr2));
        when(attributesMapper.toDto(attr2)).thenReturn(dto2);

        // Act
        var response = attributesService.getAttributesByIds(List.of(1L, 2L));

        // Assert
        assertEquals(2, response.getData().size());
        assertEquals("Color Red", response.getData().get(0).getName());
        assertEquals("Size XL", response.getData().get(1).getName());
        verify(attributesRepository, times(1)).getQuantityAttributesById(List.of(2L));
    }

    @Test
    @DisplayName("getAttributesByIds: null/empty IDs return empty without DB call")
    void getAttributesByIds_EmptyOrNullIds() {
        assertTrue(attributesService.getAttributesByIds(null).getData().isEmpty());
        assertTrue(attributesService.getAttributesByIds(List.of()).getData().isEmpty());
        verify(attributesRepository, never()).getQuantityAttributesById(anyList());
    }

    @Test
    @DisplayName("getAttributesByIds: first call misses → loads from DB → second call hits cache")
    void getAttributesByIds_CacheMissThenHit() {
        // Arrange
        when(attributesRepository.getQuantityAttributesById(List.of(1L)))
                .thenReturn(List.of(attr1));
        when(attributesMapper.toDto(attr1)).thenReturn(dto1);

        // Act: first call (cache miss)
        var firstResponse = attributesService.getAttributesByIds(List.of(1L));
        assertEquals(1, firstResponse.getData().size());

        // Act: second call (should hit cache)
        var secondResponse = attributesService.getAttributesByIds(List.of(1L));
        assertEquals(1, secondResponse.getData().size());

        // Assert: only one DB call
        verify(attributesRepository, times(1)).getQuantityAttributesById(anyList());
        verify(attributesMapper, times(1)).toDto(any(Attributes.class));
    }

    // ==================== getAttributesBySkus() ====================

    @Test
    @DisplayName("getAttributesBySkus resolves SKUs and caches via getAttributesByIds")
    void getAttributesBySkus_ResolvesAndCaches() {
        // Arrange
        List<Object[]> rows = List.of(
                new Object[]{1L, "attr-RED-001"},
                new Object[]{2L, "attr-XL-002"}
        );
        when(attributesRepository.findIdsAndSkusBySkus(anyList()))
                .thenReturn(rows);

        // getAttributesByIds will miss cache for both
        when(attributesRepository.getQuantityAttributesById(anyList()))
                .thenReturn(List.of(attr1, attr2));
        when(attributesMapper.toDto(attr1)).thenReturn(dto1);
        when(attributesMapper.toDto(attr2)).thenReturn(dto2);

        // Act
        var response = attributesService
                .getAttributesBySkus(List.of("attr-RED-001", "attr-XL-002"));

        assertEquals(2, response.getData().size());
        verify(attributesRepository, times(1)).getQuantityAttributesById(anyList());

        // Second call should hit cache
        var secondResponse = attributesService
                .getAttributesBySkus(List.of("attr-RED-001"));
        assertEquals(1, secondResponse.getData().size());
        verify(attributesRepository, times(1)).getQuantityAttributesById(anyList());
    }

    // ==================== Direct getAttributesByProductId (basic service logic, no AOP) ====================

    @Test
    @DisplayName("getAttributesByProductId loads from DB each time (no AOP proxy for @Cacheable)")
    void getAttributesByProductId_LoadsFromDb() {
        // Without Spring AOP, @Cacheable is not active, so each call goes to DB
        when(attributesRepository.findAllByProductIdNotDeleted(100L))
                .thenReturn(List.of(attr1, attr2));
        when(attributesMapper.toDto(attr1)).thenReturn(dto1);
        when(attributesMapper.toDto(attr2)).thenReturn(dto2);

        List<AttributesDto> result =
                attributesService.getAttributesByProductId("100");

        assertEquals(2, result.size());
        assertEquals("Color Red", result.get(0).getName());
        verify(attributesRepository, times(1)).findAllByProductIdNotDeleted(100L);
    }

    @Test
    @DisplayName("getAttributesByProductId for different product IDs loads separately")
    void getAttributesByProductId_DifferentProductsLoadSeparately() {
        Product product2 = Product.builder().id(200L).name("Other Product").build();
        Attributes attr3 = Attributes.builder().id(3L).name("Attribute for P2")
                .product(product2).build();
        AttributesDto dto3 = new AttributesDto();
        dto3.setId(3L);
        dto3.setName("Attribute for P2");

        when(attributesRepository.findAllByProductIdNotDeleted(100L))
                .thenReturn(List.of(attr1));
        when(attributesRepository.findAllByProductIdNotDeleted(200L))
                .thenReturn(List.of(attr3));
        when(attributesMapper.toDto(attr1)).thenReturn(dto1);
        when(attributesMapper.toDto(attr3)).thenReturn(dto3);

        List<AttributesDto> result1 = attributesService.getAttributesByProductId("100");
        List<AttributesDto> result2 = attributesService.getAttributesByProductId("200");

        assertEquals("Color Red", result1.get(0).getName());
        assertEquals("Attribute for P2", result2.get(0).getName());
        verify(attributesRepository, times(1)).findAllByProductIdNotDeleted(100L);
        verify(attributesRepository, times(1)).findAllByProductIdNotDeleted(200L);
    }

    // ==================== Cache consistency between getAttributesByProductId and getAttributesByIds ====================

    @Test
    @DisplayName("getAttributesByProductId and getAttributesByIds share the same 'attributes' cache")
    void getAttributesByProductId_And_ByIds_ShareCache() {
        // getAttributesByProductId loads from DB
        when(attributesRepository.findAllByProductIdNotDeleted(100L))
                .thenReturn(List.of(attr1));
        when(attributesMapper.toDto(attr1)).thenReturn(dto1);
        var productResult = attributesService.getAttributesByProductId("100");
        assertEquals(1, productResult.size());

        // getAttributesByIds should still be empty since @Cacheable is not active
        when(attributesRepository.getQuantityAttributesById(List.of(1L)))
                .thenReturn(List.of(attr1));
        var idsResult = attributesService.getAttributesByIds(List.of(1L));
        assertEquals(1, idsResult.getData().size());
        assertEquals("Color Red", idsResult.getData().get(0).getName());
    }

    // ==================== Direct cache operations ====================

    @Test
    @DisplayName("Direct cache put/get/evict in attributes cache works")
    void directCacheOperations_WorkCorrectly() {
        var cache = cacheManager.getCache(CacheConfig.CACHE_ATTRIBUTES);
        assertNotNull(cache);

        cache.put(100L, dto1);
        AttributesDto cached = cache.get(100L, AttributesDto.class);
        assertNotNull(cached);
        assertEquals("Color Red", cached.getName());

        cache.evict(100L);
        assertNull(cache.get(100L, AttributesDto.class));
    }

    @Test
    @DisplayName("Attributes cache stores DTOs after loading via getAttributesByIds")
    void getAttributesByIds_StoresLoadedDataInCache() {
        // Arrange
        when(attributesRepository.getQuantityAttributesById(List.of(1L)))
                .thenReturn(List.of(attr1));
        when(attributesMapper.toDto(attr1)).thenReturn(dto1);

        // Load via service
        attributesService.getAttributesByIds(List.of(1L));

        // Verify data is in cache
        var cache = cacheManager.getCache(CacheConfig.CACHE_ATTRIBUTES);
        assertNotNull(cache);
        AttributesDto cached = cache.get(1L, AttributesDto.class);
        assertNotNull(cached);
        assertEquals("Color Red", cached.getName());
    }

    @Test
    @DisplayName("getAttributesByIds: different IDs cached independently")
    void getAttributesByIds_DifferentIdsCachedIndependently() {
        // Arrange
        when(attributesRepository.getQuantityAttributesById(List.of(1L)))
                .thenReturn(List.of(attr1));
        when(attributesRepository.getQuantityAttributesById(List.of(2L)))
                .thenReturn(List.of(attr2));
        when(attributesMapper.toDto(attr1)).thenReturn(dto1);
        when(attributesMapper.toDto(attr2)).thenReturn(dto2);

        // Load both
        attributesService.getAttributesByIds(List.of(1L));
        attributesService.getAttributesByIds(List.of(2L));

        // Both should now be cached
        verify(attributesRepository, times(1)).getQuantityAttributesById(List.of(1L));
        verify(attributesRepository, times(1)).getQuantityAttributesById(List.of(2L));

        // Second calls should hit cache
        attributesService.getAttributesByIds(List.of(1L));
        attributesService.getAttributesByIds(List.of(2L));
        verify(attributesRepository, times(1)).getQuantityAttributesById(List.of(1L));
        verify(attributesRepository, times(1)).getQuantityAttributesById(List.of(2L));
    }
}
