package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.AttributesMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.PromotionMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.SpecificationMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.RedisProducerService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttributesServiceProductSkuTest {

    @Mock
    private AttributesRepository attributesRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SpecificationMapper specificationMapper;
    @Mock
    private PromotionMapper promotionMapper;
    @Mock
    private AttributesMapper attributesMapper;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private RedisProducerService redisProducerService;
    @Mock
    private MerchandiseSearchService merchandiseSearchService;

    private AttributesService attributesService;

    @BeforeEach
    void setUp() {
        attributesService = new AttributesService(
                attributesRepository,
                productRepository,
                specificationMapper,
                promotionMapper,
                attributesMapper,
                securityUtil,
                cacheManager,
                redisProducerService,
                merchandiseSearchService);
    }

    @Test
    void getAttributesByProductSkus_ResolvesProductIdsBeforeLoadingAttributes() {
        Attributes attributes = Attributes.builder().id(99L).build();
        AttributesDto attributesDto = new AttributesDto();
        attributesDto.setId(99L);

        when(productRepository.findIdsAndSkusBySkus(List.of("prd-aa")))
                .thenReturn(List.<Object[]>of(new Object[]{10L, "prd-aa"}));
        when(attributesRepository.findActiveIdsByProductIds(List.of(10L))).thenReturn(List.of(99L));
        when(cacheManager.getCache("attributes")).thenReturn(null);
        when(attributesRepository.getQuantityAttributesById(List.of(99L))).thenReturn(List.of(attributes));
        when(attributesMapper.toDto(attributes)).thenReturn(attributesDto);

        Response<List<AttributesDto>> response = attributesService.getAttributesByProductSkus(List.of("prd-aa"));

        assertThat(response.getData()).containsExactly(attributesDto);
        verify(productRepository).findIdsAndSkusBySkus(List.of("prd-aa"));
        verify(attributesRepository).findActiveIdsByProductIds(List.of(10L));
        verify(attributesRepository).getQuantityAttributesById(List.of(99L));
    }

    @Test
    void getAttributesByProductSkus_ReturnsEmptyWhenProductSkuDoesNotResolve() {
        when(productRepository.findIdsAndSkusBySkus(List.of("prd-missing"))).thenReturn(List.of());

        Response<List<AttributesDto>> response = attributesService.getAttributesByProductSkus(List.of("prd-missing"));

        assertThat(response.getData()).isEmpty();
        verify(attributesRepository, never()).findActiveIdsByProductIds(anyList());
    }
}
