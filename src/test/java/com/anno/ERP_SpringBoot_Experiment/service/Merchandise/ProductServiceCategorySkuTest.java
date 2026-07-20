package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.caffeine_cache.CacheSyncService;
import com.anno.ERP_SpringBoot_Experiment.mapper.ProductMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.MinioService;
import com.anno.ERP_SpringBoot_Experiment.service.RedisProducerService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceCategorySkuTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private MinioService minioService;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private CacheSyncService cacheSyncService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private RedisProducerService redisProducerService;
    @Mock
    private MerchandiseSearchService merchandiseSearchService;
    @Mock
    private CategoryService categoryService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                categoryRepository,
                securityUtil,
                minioService,
                productMapper,
                cacheSyncService,
                cacheManager,
                redisProducerService,
                merchandiseSearchService,
                categoryService);
    }

    @Test
    void getProductsByCategorySkus_ResolvesCategoryIdsBeforeLoadingProducts() {
        CategoryDto category = new CategoryDto();
        category.setId(10L);
        Product product = Product.builder().id(99L).build();
        ProductDto productDto = new ProductDto();
        productDto.setId(99L);

        when(categoryService.getCategoriesBySkus(List.of("ctgr-aa"))).thenReturn(Response.ok(List.of(category)));
        when(productRepository.findActiveIdsByCategoryIds(List.of(10L))).thenReturn(List.of(99L));
        when(cacheManager.getCache("productDetails")).thenReturn(null);
        when(productRepository.findActiveByIdIn(List.of(99L))).thenReturn(List.of(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        Response<List<ProductDto>> response = productService.getProductsByCategorySkus(List.of("ctgr-aa"));

        assertThat(response.getData()).containsExactly(productDto);
        verify(categoryService).getCategoriesBySkus(List.of("ctgr-aa"));
        verify(productRepository).findActiveIdsByCategoryIds(List.of(10L));
        verify(productRepository).findActiveByIdIn(List.of(99L));
    }

    @Test
    void getProductsByCategorySkus_ReturnsEmptyWhenCategorySkuDoesNotResolve() {
        when(categoryService.getCategoriesBySkus(List.of("ctgr-missing"))).thenReturn(Response.ok(List.of()));

        Response<List<ProductDto>> response = productService.getProductsByCategorySkus(List.of("ctgr-missing"));

        assertThat(response.getData()).isEmpty();
        verify(productRepository, never()).findActiveIdsByCategoryIds(org.mockito.ArgumentMatchers.anyList());
    }
}
