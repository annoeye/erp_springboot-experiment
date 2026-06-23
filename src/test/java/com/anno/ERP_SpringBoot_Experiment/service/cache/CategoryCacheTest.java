package com.anno.ERP_SpringBoot_Experiment.service.cache;

import com.anno.ERP_SpringBoot_Experiment.config.CacheConfig;
import com.anno.ERP_SpringBoot_Experiment.mapper.CategoryMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.CategoryService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.Helper;
import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.SkuInfoDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for CategoryService caching via CacheUtils.
 *
 * CategoryService.getCategoriesByIds() uses CacheUtils.getAll() to batch-load
 * CategoryDto from Caffeine cache. This test verifies:
 * - First call: cache miss → DB load → results are cached
 * - Second call: cache hit → no DB call
 * - Batch loading with mixed cache hit/miss
 * - Null/empty handling
 */
@DisplayName("Category Caching – CacheUtils via getCategoriesByIds()")
@ExtendWith(MockitoExtension.class)
class CategoryCacheTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private Helper helper;

    private CacheManager cacheManager;
    private CategoryService categoryService;

    private Category category1;
    private Category category2;
    private CategoryDto dto1;
    private CategoryDto dto2;

    @BeforeEach
    void setUp() {
        var config = new CacheConfig();
        cacheManager = config.cacheManager();

        categoryService = new CategoryService(
                categoryRepository, categoryMapper, securityUtil, helper,
                cacheManager
        );

        category1 = Category.builder().id(1L).name("Electronics")
                .skuInfo(new SkuInfo("ctgr-ELECTRO-001")).build();
        category2 = Category.builder().id(2L).name("Clothing")
                .skuInfo(new SkuInfo("ctgr-CLOTH-002")).build();

        dto1 = new CategoryDto(1L, "Electronics", new SkuInfoDto("ctgr-ELECTRO-001"), 0L);
        dto2 = new CategoryDto(2L, "Clothing", new SkuInfoDto("ctgr-CLOTH-002"), 0L);
    }

    @Test
    @DisplayName("First call: cache miss → load from DB → subsequent call hits cache")
    void getCategoriesByIds_CacheMissThenHit() {
        // Arrange: first call
        when(categoryRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(category1));
        when(categoryMapper.toDto(category1)).thenReturn(dto1);

        // Act: first call (cache miss)
        Response<List<CategoryDto>> firstResponse =
                categoryService.getCategoriesByIds(List.of(1L));

        // Assert: first call returns from DB
        assertNotNull(firstResponse);
        assertEquals(1, firstResponse.getData().size());
        assertEquals("Electronics", firstResponse.getData().get(0).getName());
        verify(categoryRepository, times(1)).findAllById(anyList());
        verify(categoryMapper, times(1)).toDto(category1);

        // Act: second call (should hit cache)
        Response<List<CategoryDto>> secondResponse =
                categoryService.getCategoriesByIds(List.of(1L));

        // Assert: second call should NOT hit DB/mapper
        assertEquals(1, secondResponse.getData().size());
        assertEquals("Electronics", secondResponse.getData().get(0).getName());
        verify(categoryRepository, times(1)).findAllById(anyList());
        verify(categoryMapper, times(1)).toDto(category1);
    }

    @Test
    @DisplayName("Batch get with mixed cached and uncached IDs")
    void getCategoriesByIds_MixedCacheHitAndMiss() {
        // Arrange: pre-cache category 1
        var cache = cacheManager.getCache(CacheConfig.CACHE_CATEGORY_DETAILS);
        assertNotNull(cache);
        cache.put(1L, dto1);

        // Only category2 needs DB load
        when(categoryRepository.findAllById(List.of(2L)))
                .thenReturn(List.of(category2));
        when(categoryMapper.toDto(category2)).thenReturn(dto2);

        // Act
        Response<List<CategoryDto>> response =
                categoryService.getCategoriesByIds(List.of(1L, 2L));

        // Assert
        assertEquals(2, response.getData().size());
        assertEquals("Electronics", response.getData().get(0).getName());
        assertEquals("Clothing", response.getData().get(1).getName());
        verify(categoryRepository, times(1)).findAllById(List.of(2L));
        verify(categoryMapper, times(1)).toDto(category2);
    }

    @Test
    @DisplayName("Null or empty IDs should return empty response without DB call")
    void getCategoriesByIds_NullOrEmptyIds_ReturnsEmpty() {
        Response<List<CategoryDto>> nullResponse =
                categoryService.getCategoriesByIds(null);
        assertTrue(nullResponse.getData().isEmpty());
        verify(categoryRepository, never()).findAllById(anyList());

        Response<List<CategoryDto>> emptyResponse =
                categoryService.getCategoriesByIds(List.of());
        assertTrue(emptyResponse.getData().isEmpty());
        verify(categoryRepository, never()).findAllById(anyList());
    }

    @Test
    @DisplayName("getCategoriesBySkus resolves SKUs and caches results via CacheUtils")
    void getCategoriesBySkus_ResolvesSkusAndCaches() {
        // Arrange
        List<Object[]> rows = List.of(
                new Object[]{1L, "ctgr-ELECTRO-001"},
                new Object[]{2L, "ctgr-CLOTH-002"}
        );
        when(categoryRepository.findIdsAndSkusBySkus(anyList()))
                .thenReturn(rows);

        // getCategoriesByIds will miss cache → load from DB
        when(categoryRepository.findAllById(anyList()))
                .thenReturn(List.of(category1, category2));
        when(categoryMapper.toDto(category1)).thenReturn(dto1);
        when(categoryMapper.toDto(category2)).thenReturn(dto2);

        // Act
        Response<List<CategoryDto>> response = categoryService
                .getCategoriesBySkus(List.of("ctgr-ELECTRO-001", "ctgr-CLOTH-002"));

        assertEquals(2, response.getData().size());
        assertEquals("Electronics", response.getData().get(0).getName());
        assertEquals("Clothing", response.getData().get(1).getName());

        // Act: second call by SKUs – should resolve from cache
        Response<List<CategoryDto>> secondResponse = categoryService
                .getCategoriesBySkus(List.of("ctgr-ELECTRO-001"));

        assertEquals(1, secondResponse.getData().size());
        assertEquals("Electronics", secondResponse.getData().get(0).getName());
        verify(categoryRepository, times(1)).findAllById(anyList());
    }

    @Test
    @DisplayName("Cache miss for single ID triggers DB load exactly once")
    void getCategoriesByIds_SingleIdMiss_LoadsFromDbOnce() {
        // Arrange
        when(categoryRepository.findAllById(List.of(99L)))
                .thenReturn(List.of(Category.builder().id(99L).name("NewCat")
                        .skuInfo(new SkuInfo("ctgr-NEW-099")).build()));
        when(categoryMapper.toDto(any(Category.class)))
                .thenReturn(new CategoryDto(99L, "NewCat", new SkuInfoDto("ctgr-NEW-099"), 0L));

        // Act: multiple calls with same ID
        categoryService.getCategoriesByIds(List.of(99L));
        categoryService.getCategoriesByIds(List.of(99L));
        categoryService.getCategoriesByIds(List.of(99L));

        // Assert: only one DB call (cached after first)
        verify(categoryRepository, times(1)).findAllById(anyList());
        verify(categoryMapper, times(1)).toDto(any(Category.class));
    }

    @Test
    @DisplayName("Cache stores DTOs – getCategoriesByIds returns cached data after first load")
    void getCategoriesByIds_CacheStoresDtoForSubsequentCalls() {
        // Arrange
        when(categoryRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(category1));
        when(categoryMapper.toDto(category1)).thenReturn(dto1);

        // Load into cache via service
        categoryService.getCategoriesByIds(List.of(1L));

        // Directly verify cache contains the DTO
        var cache = cacheManager.getCache(CacheConfig.CACHE_CATEGORY_DETAILS);
        assertNotNull(cache);
        CategoryDto cached = cache.get(1L, CategoryDto.class);
        assertNotNull(cached);
        assertEquals("Electronics", cached.getName());
    }
}
