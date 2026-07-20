package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.AttributesMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.PromotionMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.SpecificationMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SpecificationGroup;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.VariantOption;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributesSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.RedisProducerService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iAttributes;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.caffeine_cache.CacheConfig;
import com.anno.ERP_SpringBoot_Experiment.caffeine_cache.CacheUtils;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class AttributesService implements iAttributes {

    private final AttributesRepository attributesRepository;
    private final ProductRepository productRepository;
    private final SpecificationMapper specificationMapper;
    private final PromotionMapper promotionMapper;
    private final AttributesMapper attributesMapper;
    private final SecurityUtil securityUtil;
    private final org.springframework.cache.CacheManager cacheManager;
    private final RedisProducerService redisProducerService;
    private final MerchandiseSearchService merchandiseSearchService;

    @Override
    @Transactional
    @CacheEvict(value = "attributes", allEntries = true)
    public Response<List<AttributesDto>> create(@NonNull CreateAttributesRequest request) {
        Product product = productRepository
                .findProductBySkuInfo_Sku(request.getProductSku())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        List<Attributes> attributesList = new ArrayList<>();
        
        for (com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributeInput item : request.getAttributes()) {
            Attributes attr = Attributes.builder()
                    .product(product)
                    .sku(SkuInfo.builder()
                            .sku(new SkuInfo().createSku("attr-").getSku()
                                    .replaceFirst("-", "-" + request.getProductSku().substring(request.getProductSku().length() - 3)))
                            .build())
                    .name(request.getName())
                    .price(item.getPrice() != null ? item.getPrice().doubleValue() : 0.0)
                    .salePrice(item.getSalePrice() != null ? item.getSalePrice().doubleValue() : 0.0)
                    .variantOptions((item.getVariantOptions() != null && !item.getVariantOptions().isEmpty())
                            ? item.getVariantOptions().stream()
                                    .map(dto -> new VariantOption(dto.getName(), dto.getValues()))
                                    .toList()
                            : new ArrayList<>())
                    .keywords(request.getKeywords())
                    .promotions((item.getPromotions() != null && !item.getPromotions().isEmpty())
                            ? item.getPromotions()
                            : new ArrayList<>())
                    .specifications((item.getSpecifications() != null && !item.getSpecifications().isEmpty())
                            ? item.getSpecifications()
                            : new ArrayList<>())
                    .statusProduct(item.getStatusProduct() != null ? item.getStatusProduct() : StockStatus.NOT_ACTIVE)
                    .build();

            attributesList.add(attr);
        }

        List<Attributes> savedList = attributesRepository.saveAll(attributesList);

        log.info("Đã tạo {} attributes cho sản phẩm {}",
                savedList.size(),
                product.getName());

        String message = savedList.size() == 1
                ? "Tạo attributes '"+ savedList.getFirst().getName() + "' thành công."
                : String.format("Đã tạo thành công %d variants.", savedList.size());

        attributesMapper.toDto(savedList);
        
        // Hook: Gửi yêu cầu xóa cache bất đồng bộ qua Redis Stream
        redisProducerService.sendEvictMessage(product.getId().toString());

        return Response.ok(message);
    }

    @Override
    @Transactional
    @CacheEvict(value = "attributes", allEntries = true)
    public Response<?> update(@NonNull UpdateAttributesRequest request) {

        Attributes attributes = attributesRepository
                .findById(Long.valueOf(request.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                        "Thuộc tính sản phẩm không tồn tại."));

        if (request.getName() != null && !request.getName().isBlank()) {
            attributes.setName(request.getName());
        }

        if (request.getPrice() != null) {
            if (request.getPrice() < 0) {
                throw new BusinessException(ErrorCode.INVALID_PRICE, "Giá của sản phẩm thể là số âm.");
            }
            attributes.setPrice(request.getPrice());
        }

        if (request.getSalePrice() != null) {
            if (request.getSalePrice() < 0) {
                throw new BusinessException(ErrorCode.INVALID_PRICE, "Giá khuyến mãi không thể là số âm.");
            }
            double currentPrice = request.getPrice() != null ? request.getPrice() : attributes.getPrice();
            if (request.getSalePrice() > currentPrice) {
                throw new BusinessException(ErrorCode.INVALID_PRICE, "Giá khuyến mãi không thể lớn hơn giá gốc.");
            }
            attributes.setSalePrice(request.getSalePrice());
        }

        if (request.getVariantOptions() != null && !request.getVariantOptions().isEmpty()) {
            List<VariantOption> variantOptions = request.getVariantOptions().stream()
                    .map(dto -> new VariantOption(dto.getName(), dto.getValues()))
                    .toList();
            attributes.setVariantOptions(variantOptions);
        }

        if (request.getKeywords() != null) {
            attributes.setKeywords(new HashSet<>(request.getKeywords()));
        }

        if (request.getSpecifications() != null) {
            List<SpecificationGroup> groups = request.getSpecifications().stream()
                    .map(dto -> SpecificationGroup.builder()
                            .groupName(dto.getGroupName())
                            .specifications(dto.getSpecifications() != null
                                    ? dto.getSpecifications().stream()
                                            .map(specificationMapper::toEntity)
                                            .toList()
                                    : new ArrayList<>())
                            .build())
                    .toList();
            attributes.setSpecifications(groups);
        }

        if (request.getPromotions() != null) {
            attributes.getPromotions().clear();
            attributes.getPromotions().addAll(
                    promotionMapper.toEntity(request.getPromotions()));
        }

        if (request.getStatus() != null) {
            attributes.setStatusProduct(request.getStatus());
        }

        attributes.addUpdateEntry("Cập nhật thuộc tính sản phẩm", securityUtil.getCurrentUsername());

        attributesRepository.save(attributes);

        // Hook: Gửi yêu cầu xóa cache bất đồng bộ qua Redis Stream
        if (attributes.getProduct() != null) {
            redisProducerService.sendEvictMessage(attributes.getProduct().getId().toString());
        }

        return Response.ok("Đã cập nhật thành công.");
    }

    @Override
    @Transactional
    @CacheEvict(value = "attributes", allEntries = true)
    public Response<?> delete(@NonNull List<String> ids) {
        if (ids.isEmpty()) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND, "Mã định danh không được để trống.");
        }

        List<Long> attrUuids = ids.stream()
                .map(Long::valueOf)
                .toList();

        List<Attributes> attributesToDelete = attributesRepository.findAllById(attrUuids);

        if (attributesToDelete.isEmpty()) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                    "Không tìm thấy Danh mục với mã định danh đã cung cấp.");
        }

        if (attributesToDelete.size() != ids.size()) {
            log.warn("Một số SKU không tồn tại. Yêu cầu: {}, Tìm thấy: {}",
                    ids.size(),
                    attributesToDelete.size());
        }

        String currentUser = securityUtil.getCurrentUsername();

        // Soft delete
        attributesToDelete.forEach(attr -> {
            attr.markDeletedAfter30Days(currentUser);
        });

        attributesRepository.saveAll(attributesToDelete);

        log.info("Đã xóa {} attributes", attributesToDelete.size());

        // Hook: Gửi yêu cầu xóa cache bất đồng bộ qua Redis Stream cho các sản phẩm liên quan
        attributesToDelete.stream()
                .map(Attributes::getProduct)
                .filter(Objects::nonNull)
                .map(Product::getId)
                .distinct()
                .forEach(productId -> redisProducerService.sendEvictMessage(productId.toString()));

        return Response.noContent();
    }

    @Override
    @Transactional
    @CacheEvict(value = "attributes", allEntries = true)
    public Response<?> deleteByProduct(@NonNull String productId) {
        Product product = productRepository.findById(Long.valueOf(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        List<Attributes> attributesList = attributesRepository.findAllByProduct(product);

        if (attributesList.isEmpty()) {
            return Response.ok(null, "Sản phẩm không có danh mục nào để xóa.");
        }

        String currentUser = securityUtil.getCurrentUsername();

        attributesList.forEach(attr -> {
            attr.markDeletedAfter30Days(currentUser);
        });

        attributesRepository.saveAll(attributesList);

        log.info("Đã xóa {} attributes của sản phẩm {} bởi user {}",
                attributesList.size(),
                product.getName(),
                currentUser);

        // Hook: Gửi yêu cầu xóa cache bất đồng bộ qua Redis Stream
        redisProducerService.sendEvictMessage(productId);

        return Response.ok(
                null,
                String.format("Đã xóa thành công %d danh mục của sản phẩm.", attributesList.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttributesDto> search(@NonNull AttributesSearchRequest request) {
        List<String> productSkuFilters = collectProductSkus(request);
        List<Long> productIdsFromSkus = resolveProductIdsFromSkus(productSkuFilters);
        if (!productSkuFilters.isEmpty() && productIdsFromSkus.isEmpty()) {
            var pageable = merchandiseSearchService.pageable(request.getPaging());
            return Page.empty(pageable);
        }

        AttributesSearchRequest resolvedRequest = withResolvedProductIds(request, productIdsFromSkus);
        List<Long> ids = merchandiseSearchService.searchAttributeIds(resolvedRequest);
        List<AttributesDto> content = getAttributesByIds(ids).getData();

        var pageable = merchandiseSearchService.pageable(resolvedRequest.getPaging());

        long total;
        total = attributesRepository.count(merchandiseSearchService.attributesSpecification(resolvedRequest));

        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }


    @Override
    @Transactional(readOnly = true)
    public Response<List<AttributesDto>> getAttributesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Response.ok(new ArrayList<>());
        }

        Map<Long, AttributesDto> dtoMap = CacheUtils.getAll(
                cacheManager,
                CacheConfig.CACHE_ATTRIBUTES,
                ids,
                missingIds -> attributesRepository.getQuantityAttributesById(new ArrayList<>(missingIds)).stream()
                        .collect(Collectors.toMap(Attributes::getId, attributesMapper::toDto))
        );

        List<AttributesDto> result = ids.stream()
                .map(dtoMap::get)
                .filter(Objects::nonNull)
                .toList();

        return Response.ok(result);
    }

@Override
@Transactional(readOnly = true)
public Response<List<AttributesDto>> getAttributesBySkus(List<String> skus) {
    if (skus == null || skus.isEmpty()) {
        return Response.ok(new ArrayList<>());
    }

    List<Object[]> rows = attributesRepository.findIdsAndSkusBySkus(skus);
    Map<String, Long> skuToIdMap = rows.stream()
            .collect(Collectors.toMap(
                    row -> (String) row[1], // Key là SKU
                    row -> (Long) row[0],   // Value là ID
                    (existing, replacement) -> existing 
            ));

    List<Long> ids = skus.stream()
            .map(skuToIdMap::get)   
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (ids.isEmpty()) {
        return Response.ok(new ArrayList<>());
    }

    List<AttributesDto> dtos = getAttributesByIds(ids).getData();

    Map<String, AttributesDto> skuToDtoMap = dtos.stream()
            .filter(dto -> dto.getSku() != null && dto.getSku().getSku() != null)
            .collect(Collectors.toMap(
                    dto -> dto.getSku().getSku(),
                    dto -> dto,                 
                    (existing, replacement) -> existing
            ));

    return Response.ok(skus.stream()
            .map(skuToDtoMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Response<List<AttributesDto>> getAttributesByProductSkus(List<String> productSkus) {
        List<String> filters = filterSkus(productSkus);
        if (filters.isEmpty()) {
            return Response.ok(new ArrayList<>());
        }

        List<Long> productIds = resolveProductIdsFromSkus(filters);
        if (productIds.isEmpty()) {
            return Response.ok(new ArrayList<>());
        }

        List<Long> attributeIds = attributesRepository.findActiveIdsByProductIds(productIds);
        return getAttributesByIds(attributeIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> searchAttributesIds(@NonNull AttributesSearchRequest request) {
        List<String> productSkuFilters = collectProductSkus(request);
        List<Long> productIdsFromSkus = resolveProductIdsFromSkus(productSkuFilters);
        if (!productSkuFilters.isEmpty() && productIdsFromSkus.isEmpty()) {
            return List.of();
        }
        return merchandiseSearchService.searchAttributeIds(withResolvedProductIds(request, productIdsFromSkus));
    }

    @Override
    @Cacheable(value = "attributes", key = "#productId")
    public List<AttributesDto> getAttributesByProductId(String productId) {
        log.info("Cache miss! Query DB lấy thuộc tính cho sản phẩm ID: {}", productId);
        return attributesRepository
                .findAllByProductIdNotDeleted(Long.valueOf(productId))
                .stream()
                .map(attributesMapper::toDto)
                .toList();
    }

    private List<String> collectProductSkus(AttributesSearchRequest request) {
        List<String> filters = new ArrayList<>();
        if (org.springframework.util.StringUtils.hasText(request.getProductSku())) {
            filters.add(request.getProductSku().trim());
        }
        filters.addAll(filterSkus(request.getProductSkus()));
        return filters.stream().distinct().toList();
    }

    private List<String> filterSkus(List<String> skus) {
        if (skus == null || skus.isEmpty()) {
            return List.of();
        }
        return skus.stream()
                .filter(org.springframework.util.StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<Long> resolveProductIdsFromSkus(List<String> productSkus) {
        if (productSkus == null || productSkus.isEmpty()) {
            return List.of();
        }

        Map<String, Long> skuToIdMap = productRepository.findIdsAndSkusBySkus(productSkus).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[1],
                        row -> (Long) row[0],
                        (existing, replacement) -> existing
                ));

        return productSkus.stream()
                .map(skuToIdMap::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private AttributesSearchRequest withResolvedProductIds(
            AttributesSearchRequest request,
            List<Long> productIdsFromSkus) {
        if (productIdsFromSkus == null || productIdsFromSkus.isEmpty()) {
            return request;
        }

        List<String> mergedProductIds = new ArrayList<>();
        if (request.getProductIds() != null) {
            mergedProductIds.addAll(request.getProductIds());
        }
        mergedProductIds.addAll(productIdsFromSkus.stream().map(String::valueOf).toList());

        AttributesSearchRequest resolved = new AttributesSearchRequest();
        resolved.setKeyword(request.getKeyword());
        resolved.setProductId(request.getProductId());
        resolved.setProductSku(request.getProductSku());
        resolved.setIds(request.getIds());
        resolved.setProductIds(mergedProductIds.stream().distinct().toList());
        resolved.setProductSkus(request.getProductSkus());
        resolved.setSkus(request.getSkus());
        resolved.setStatuses(request.getStatuses());
        resolved.setMinPrice(request.getMinPrice());
        resolved.setMaxPrice(request.getMaxPrice());
        resolved.setMinSalePrice(request.getMinSalePrice());
        resolved.setMaxSalePrice(request.getMaxSalePrice());
        resolved.setMinSoldQuantity(request.getMinSoldQuantity());
        resolved.setMaxSoldQuantity(request.getMaxSoldQuantity());
        resolved.setMinCostPrice(request.getMinCostPrice());
        resolved.setMaxCostPrice(request.getMaxCostPrice());
        resolved.setCreatedBy(request.getCreatedBy());
        resolved.setCreatedFrom(request.getCreatedFrom());
        resolved.setCreatedTo(request.getCreatedTo());
        resolved.setUpdatedFrom(request.getUpdatedFrom());
        resolved.setUpdatedTo(request.getUpdatedTo());
        resolved.setPaging(request.getPaging());
        return resolved;
    }
}
