package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.AttributesMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.PromotionMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.SpecificationMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SpecificationGroup;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.Specificationa;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.VariantOption;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SearchCriteria;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SpecificationBuilder;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributesSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.VariantGroupInput;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.VariantValueInput;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iAttributes;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final Helper featureMerchandiseHelper;
    private final SpecificationMapper specificationMapper;
    private final PromotionMapper promotionMapper;
    private final AttributesMapper attributesMapper;
    private final SecurityUtil securityUtil;
    private final org.springframework.cache.CacheManager cacheManager;
    private final jakarta.persistence.EntityManager entityManager;

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
        return Response.noContent();
    }

    @Override
    @Transactional
    @CacheEvict(value = "attributes", key = "#productId")
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

        return Response.ok(
                null,
                String.format("Đã xóa thành công %d danh mục của sản phẩm.", attributesList.size()));
    }

    private List<SearchCriteria> buildAttributesSearchCriteria(AttributesSearchRequest request) {
        List<SearchCriteria> criteriaList = new ArrayList<>();

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            criteriaList.add(new SearchCriteria("name", "~", request.getKeyword()));
        }

        if (request.getIds() != null && !request.getIds().isEmpty()) {
            List<Long> attrIds = request.getIds().stream()
                    .map(Long::valueOf)
                    .toList();
            criteriaList.add(new SearchCriteria("id", "~", attrIds));
        }

        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            List<Long> prodIds = request.getProductIds().stream()
                    .map(Long::valueOf)
                    .toList();
            criteriaList.add(new SearchCriteria("product.id", "~", prodIds));
        }

        if (request.getProductId() != null && !request.getProductId().isBlank()) {
            criteriaList.add(new SearchCriteria("product.id", ":", Long.valueOf(request.getProductId().trim())));
        }

        if (request.getSkus() != null && !request.getSkus().isEmpty()) {
            criteriaList.add(new SearchCriteria("sku.sku", "~", request.getSkus()));
        }

        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            criteriaList.add(new SearchCriteria("statusProduct", "~", request.getStatuses()));
        }

        if (request.getMinPrice() != null) {
            criteriaList.add(new SearchCriteria("price", ">", request.getMinPrice()));
        }
        if (request.getMaxPrice() != null) {
            criteriaList.add(new SearchCriteria("price", "<", request.getMaxPrice()));
        }

        if (request.getMinSalePrice() != null) {
            criteriaList.add(new SearchCriteria("salePrice", ">", request.getMinSalePrice()));
        }
        if (request.getMaxSalePrice() != null) {
            criteriaList.add(new SearchCriteria("salePrice", "<", request.getMaxSalePrice()));
        }

        if (request.getMinSoldQuantity() != null) {
            criteriaList.add(new SearchCriteria("soldQuantity", ">", request.getMinSoldQuantity()));
        }
        if (request.getMaxSoldQuantity() != null) {
            criteriaList.add(new SearchCriteria("soldQuantity", "<", request.getMaxSoldQuantity()));
        }

        if (request.getMinCostPrice() != null) {
            criteriaList.add(new SearchCriteria("costPrice", ">", request.getMinCostPrice()));
        }
        if (request.getMaxCostPrice() != null) {
            criteriaList.add(new SearchCriteria("costPrice", "<", request.getMaxCostPrice()));
        }

        if (request.getCreatedBy() != null && !request.getCreatedBy().isEmpty()) {
            criteriaList.add(new SearchCriteria("auditInfo.createdBy", "~", request.getCreatedBy()));
        }

        if (request.getCreatedFrom() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.createdAt", ">", request.getCreatedFrom()));
        }
        if (request.getCreatedTo() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.createdAt", "<", request.getCreatedTo()));
        }
        if (request.getUpdatedFrom() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.updatedAt", ">", request.getUpdatedFrom()));
        }
        if (request.getUpdatedTo() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.updatedAt", "<", request.getUpdatedTo()));
        }

        return criteriaList;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "attributes", key = "#request.hashCode()")
    public Page<AttributesDto> search(@NonNull AttributesSearchRequest request) {
        List<Long> ids = searchAttributesIds(request);
        List<AttributesDto> content = getAttributesByIds(ids).getData();

        List<SearchCriteria> criteriaList = buildAttributesSearchCriteria(request);
        SpecificationBuilder<Attributes> builder = new SpecificationBuilder<>(criteriaList);
        Specification<Attributes> spec = builder.build();

        Pageable pageable = (request.getPaging() != null) ? request.getPaging().pageable() : PageRequest.of(0, 10);

        long total;
        if (spec != null) {
            total = attributesRepository.count(spec);
        } else {
            total = attributesRepository.count();
        }

        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> searchAttributesIds(@NonNull AttributesSearchRequest request) {
        List<SearchCriteria> criteriaList = buildAttributesSearchCriteria(request);
        SpecificationBuilder<Attributes> builder = new SpecificationBuilder<>(criteriaList);
        Specification<Attributes> spec = builder.build();

        jakarta.persistence.criteria.CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        jakarta.persistence.criteria.CriteriaQuery<Long> query = cb.createQuery(Long.class);
        jakarta.persistence.criteria.Root<Attributes> root = query.from(Attributes.class);

        query.select(root.get("id"));

        if (spec != null) {
            jakarta.persistence.criteria.Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        // Apply paging if specified
        jakarta.persistence.TypedQuery<Long> typedQuery = entityManager.createQuery(query);
        if (request.getPaging() != null) {
            Pageable pageable = request.getPaging().pageable();
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        return typedQuery.getResultList();
    }



    // Removed mapSpecificationGroups as it is no longer needed

    // Lazy Loading: Lấy danh sách thuộc tính theo Product ID.
    // Dữ liệu được nạp vào RAM khi lần đầu gọi (Cache Miss) và tự hết hạn sau 5 phút (cấu hình trong CacheConfig).
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

    @Override
    @Transactional(readOnly = true)
    public Response<List<AttributesDto>> getAttributesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Response.ok(new java.util.ArrayList<>());
        }

        org.springframework.cache.Cache cache = cacheManager.getCache("attributes");
        java.util.Map<Long, AttributesDto> dtoMap;

        if (cache != null) {
            @SuppressWarnings("unchecked")
            com.github.benmanes.caffeine.cache.Cache<Long, AttributesDto> nativeCache =
                    (com.github.benmanes.caffeine.cache.Cache<Long, AttributesDto>) cache.getNativeCache();

            dtoMap = nativeCache.getAll(ids, missingIds -> {
                List<Long> missingList = new java.util.ArrayList<>(missingIds);
                List<Attributes> attributesList = attributesRepository.getQuantityAttributesById(missingList);
                java.util.Map<Long, AttributesDto> loaded = new java.util.HashMap<>();
                for (Attributes a : attributesList) {
                    loaded.put(a.getId(), attributesMapper.toDto(a));
                }
                return loaded;
            });
        } else {
            List<Attributes> attributesList = attributesRepository.getQuantityAttributesById(ids);
            dtoMap = new java.util.HashMap<>();
            for (Attributes a : attributesList) {
                dtoMap.put(a.getId(), attributesMapper.toDto(a));
            }
        }

        // Reconstruct list preserving the original requested order
        List<AttributesDto> result = new java.util.ArrayList<>();
        for (Long id : ids) {
            AttributesDto dto = dtoMap.get(id);
            if (dto != null) {
                result.add(dto);
            }
        }

        return Response.ok(result);
    }
}
