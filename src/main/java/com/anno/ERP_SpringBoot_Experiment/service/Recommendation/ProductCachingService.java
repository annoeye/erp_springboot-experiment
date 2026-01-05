package com.anno.ERP_SpringBoot_Experiment.service.Recommendation;

import com.anno.ERP_SpringBoot_Experiment.mapper.ProductMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.CachingStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.RedisService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductCachingDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iProductCaching;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCachingService implements iProductCaching {

    private final RedisService redisService;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public void addProduct(List<ProductDto> items) {

        String recommendationId = UUID.randomUUID().toString();

        String key = "rec:" + recommendationId;

        Set<UUID> productIds = items.stream()
                .map(ProductDto::getId)
                .collect(Collectors.toSet());

        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new BusinessException("Sản phẩm không tồn tại.");
        }

        ProductCachingDto productCachingDto = ProductCachingDto.builder()
                .recommendationId(recommendationId)
                .strategy(String.valueOf(CachingStatus.PENDING))
                .items(productMapper.toDto(products))
                .generatedAt(System.currentTimeMillis())
                .build();

        redisService.setValue(key, productCachingDto);
    }
}
