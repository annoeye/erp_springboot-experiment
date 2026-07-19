package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.ProductMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.MinioService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.GetProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ProductIsExiting;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.RedisProducerService;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iProduct;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements iProduct {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SecurityUtil securityUtil;
    private final Helper featureMerchandiseHelper;
    private final MinioService minioService;
    private final ProductMapper productMapper;
    private final com.anno.ERP_SpringBoot_Experiment.caffeine_cache.CacheSyncService cacheSyncService;
    private final org.springframework.cache.CacheManager cacheManager;
    private final RedisProducerService redisProducerService;

    @Override
    public Response<?> addProduct(CreateProductRequest request) {
        Category category = categoryRepository
                .findCategoryBySkuInfo_Sku(request.getCategorySku())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục không tồn tại."));

        productRepository.save(
                Product.builder()
                        .name(request.getName())
                        .category(category)
                        .skuInfo(SkuInfo.builder()
                                .sku(new SkuInfo().createSku("prd-").getSku()
                                        .replaceFirst("-",
                                                "-" + request.getCategorySku()
                                                        .substring(request.getCategorySku().length() - 2)))
                                .build())
                        .createdAt(LocalDateTime.now())
                        .createdBy(securityUtil.getCurrentUsername())
                        .status(Stream.of(ActiveStatus.ACTIVE, ActiveStatus.LOCKED)
                                .filter(s -> s.name().equals(request.getStatus()))
                                .findFirst()
                                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                                        "Định dạng không hợp lệ!")))
                        .build());
        return Response.ok("Thêm sản phẩm '" + request.getName() + "' thành công.");
    }

    @Override
    @Transactional
    public Response<?> updateProduct(UpdateProductRequest request) {
        if (!StringUtils.hasText(request.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Sản phẩm không không được để trống.");
        }

        final var product = productRepository.findById(Long.valueOf(request.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        if (StringUtils.hasText(request.getCategoryId())) {
            Category category = categoryRepository
                    .findCategoryById(Long.valueOf(request.getCategoryId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục không tồn tại."));
            product.setCategory(category);
        }

        productMapper.updateFromRequest(request, product);

        product.addUpdateEntry("Cập nhật thông tin sản phẩm", securityUtil.getCurrentUsername());

        log.info("Đã cập nhật sản phẩm '{}' với ID {}", product.getName(), product.getId());
        productRepository.save(product);

        // Hook: Gửi yêu cầu xóa cache bất đồng bộ qua Redis Stream
        redisProducerService.sendEvictMessage(product.getId().toString());

        // Hook: Báo hiệu cho luồng đồng bộ chạy ngầm cập nhật cache RAM
        cacheSyncService.markProductDirty(product.getId());

        return Response.ok("Cập nhật sản phẩm thành công.");
    }

    @Override
    @CacheEvict(value = "productDetails", allEntries = true)
    public Response<?> deleteProduct(@NonNull final List<Long> ids) {
        // Xóa mềm danh sách sản phẩm
        productRepository.softDeleteAllByIds(ids, securityUtil.getCurrentUsername());
        // Hook: Gửi yêu cầu xóa cache bất đồng bộ qua Redis Stream
        ids.forEach(id -> redisProducerService.sendEvictMessage(id.toString()));
        return Response.noContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> searchProducts(@NonNull GetProductRequest request) {
        var pageable = request.getPaging() != null ? request.getPaging().pageable() : PageRequest.of(0, 10);
        return productRepository.findAll(buildProductSpecification(request), pageable)
                .map(productMapper::toDto);
    }

    @Override
    public ProductIsExiting isExiting(String name) {
        return productRepository.findProductByName(name)
                .map(p -> ProductIsExiting
                        .builder()
                        .id(String.valueOf(p.getId()))
                        .isExiting(true)
                        .build())
                .orElseGet(() -> ProductIsExiting
                        .builder()
                        .id(null)
                        .isExiting(false)
                        .build());
    }

    // Lazy Load: Lấy chi tiết sản phẩm theo ID từ cache RAM.
    // Lần đầu gọi (Cache Miss) sẽ query DB bằng JOIN FETCH để tải Product + Category trong 1 câu SQL rồi lưu vào RAM. Những lần sau lấy thẳng từ RAM.
    @Cacheable(value = "productDetails", key = "#id")
    public ProductDto getProductById(Long id) {
        log.info("Cache miss! Query DB lấy thông tin sản phẩm ID: {}", id);
        return productRepository.findByIdWithDetails(id)
                .map(productMapper::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));
    }

    @Override
    public void viewCount(String productId) {
        productRepository.updateViewCount(Long.valueOf(productId));
    }

    @Override
    public void totalSoldQuantity(String productId) {
        productRepository.updateTotalSoldQuantity(
                Long.valueOf(productId),
                1);
    }

    @Override
    public void totalRevenue(String productId, double price) {
        productRepository.updateTotalRevenue(
                Long.valueOf(productId),
                BigDecimal.valueOf(price));
    }

    @Override
    @Transactional(readOnly = true)
    public Response<List<ProductDto>> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Response.ok(new java.util.ArrayList<>());
        }

        org.springframework.cache.Cache cache = cacheManager.getCache("productDetails");
        java.util.Map<Long, ProductDto> dtoMap;

        if (cache != null) {
            @SuppressWarnings("unchecked")
            com.github.benmanes.caffeine.cache.Cache<Long, ProductDto> nativeCache =
                    (com.github.benmanes.caffeine.cache.Cache<Long, ProductDto>) cache.getNativeCache();

            dtoMap = nativeCache.getAll(ids, missingIds -> {
                List<Long> missingList = new java.util.ArrayList<>(missingIds);
                List<Product> products = productRepository.findActiveByIdIn(missingList);
                java.util.Map<Long, ProductDto> loaded = new java.util.HashMap<>();
                for (Product p : products) {
                    loaded.put(p.getId(), productMapper.toDto(p));
                }
                return loaded;
            });
        } else {
            List<Product> products = productRepository.findActiveByIdIn(ids);
            dtoMap = new java.util.HashMap<>();
            for (Product p : products) {
                dtoMap.put(p.getId(), productMapper.toDto(p));
            }
        }

        // Reconstruct list preserving the original requested order
        List<ProductDto> result = new java.util.ArrayList<>();
        for (Long id : ids) {
            ProductDto dto = dtoMap.get(id);
            if (dto != null) {
                result.add(dto);
            }
        }

        return Response.ok(result);
    }

    @Override
    @Transactional(readOnly = true)
    public Response<List<ProductDto>> getProductsBySkus(List<String> skus) {
        if (skus == null || skus.isEmpty()) {
            return Response.ok(new java.util.ArrayList<>());
        }

        List<Object[]> rows = productRepository.findIdsAndSkusBySkus(skus);
        java.util.Map<String, Long> skuToIdMap = new java.util.HashMap<>();
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            String s = (String) row[1];
            skuToIdMap.put(s, id);
        }

        List<Long> ids = new java.util.ArrayList<>();
        for (String s : skus) {
            Long id = skuToIdMap.get(s);
            if (id != null) {
                ids.add(id);
            }
        }

        List<ProductDto> dtos = getProductsByIds(ids).getData();

        java.util.Map<String, ProductDto> skuToDtoMap = new java.util.HashMap<>();
        for (ProductDto dto : dtos) {
            if (dto.getSkuInfo() != null && dto.getSkuInfo().getSku() != null) {
                skuToDtoMap.put(dto.getSkuInfo().getSku(), dto);
            }
        }

        List<ProductDto> result = new java.util.ArrayList<>();
        for (String s : skus) {
            ProductDto dto = skuToDtoMap.get(s);
            if (dto != null) {
                result.add(dto);
            }
        }

        return Response.ok(result);
    }

    private Specification<Product> buildProductSpecification(GetProductRequest request) {
        Specification<Product> specification = activeProductsOnly();

        specification = specification.and(keywordSpecification(request.getKeyword()));
        specification = specification.and(equalsIfPresent("createdBy", request.getCreatedBy()));
        specification = specification.and(equalsLongIfPresent("category.id", request.getCategoryId()));
        specification = specification.and(inLongListIfPresent("id", featureMerchandiseHelper.filterBlank(request.getProductIds())));
        specification = specification.and(inStringListIfPresent("skuInfo.sku", request.getSkus()));
        specification = specification.and(inStringListIfPresent("status", request.getStatuses()));
        specification = specification.and(inLongListIfPresent("category.id", featureMerchandiseHelper.filterBlank(request.getCategoryIds())));
        specification = specification.and(greaterThanOrEqualTo("totalSoldQuantity", request.getMinSoldQuantity()));
        specification = specification.and(lessThanOrEqualTo("totalSoldQuantity", request.getMaxSoldQuantity()));
        specification = specification.and(greaterThanOrEqualTo("totalRevenue", request.getMinRevenue() == null ? null : BigDecimal.valueOf(request.getMinRevenue())));
        specification = specification.and(lessThanOrEqualTo("totalRevenue", request.getMaxRevenue() == null ? null : BigDecimal.valueOf(request.getMaxRevenue())));
        specification = specification.and(greaterThanOrEqualTo("totalOrders", request.getMinOrders()));
        specification = specification.and(lessThanOrEqualTo("totalOrders", request.getMaxOrders()));
        specification = specification.and(greaterThanOrEqualTo("viewCount", request.getMinView()));
        specification = specification.and(greaterThanOrEqualTo("averageRating", request.getMinRating()));
        specification = specification.and(greaterThanOrEqualTo("reviewCount", request.getMinReviews()));
        specification = specification.and(greaterThanOrEqualTo("createdAt", request.getCreatedFrom()));
        specification = specification.and(lessThanOrEqualTo("createdAt", request.getCreatedTo()));
        specification = specification.and(greaterThanOrEqualTo("updatedAt", request.getUpdatedFrom()));
        specification = specification.and(lessThanOrEqualTo("updatedAt", request.getUpdatedTo()));

        return specification;
    }

    private Specification<Product> activeProductsOnly() {
        return (root, query, cb) -> cb.and(
                cb.or(
                        cb.isNull(root.get("isDeleted")),
                        cb.isFalse(root.get("isDeleted"))),
                cb.or(
                        cb.isNull(root.get("deletedAt")),
                        cb.greaterThan(root.get("deletedAt"), LocalDateTime.now())));
    }

    private Specification<Product> keywordSpecification(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            query.distinct(true);

            Join<Product, Attributes> attributesJoin = root.join("attributes", JoinType.LEFT);
            Join<Attributes, String> keywordsJoin = attributesJoin.join("keywords", JoinType.LEFT);

            Predicate productName = cb.like(cb.lower(root.get("name")), normalizedKeyword);
            Predicate productSku = cb.like(cb.lower(root.get("skuInfo").get("sku")), normalizedKeyword);
            Predicate attributeName = cb.like(cb.lower(attributesJoin.get("name")), normalizedKeyword);
            Predicate attributeSku = cb.like(cb.lower(attributesJoin.get("sku").get("sku")), normalizedKeyword);
            Predicate attributeKeyword = cb.like(cb.lower(keywordsJoin), normalizedKeyword);

            return cb.or(productName, productSku, attributeName, attributeSku, attributeKeyword);
        };
    }

    private Specification<Product> equalsIfPresent(String path, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalizedValue = value.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(resolvePath(root, path).as(String.class)), normalizedValue);
    }

    private Specification<Product> equalsLongIfPresent(String path, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        Long parsedValue = Long.valueOf(value.trim());
        return (root, query, cb) -> cb.equal(resolvePath(root, path).as(Long.class), parsedValue);
    }

    private Specification<Product> inStringListIfPresent(String path, List<String> values) {
        List<String> filteredValues = featureMerchandiseHelper.filterBlank(values).stream()
                .map(String::trim)
                .toList();
        if (filteredValues.isEmpty()) {
            return null;
        }

        return (root, query, cb) -> resolvePath(root, path).in(filteredValues);
    }

    private Specification<Product> inLongListIfPresent(String path, List<String> values) {
        List<Long> parsedValues = featureMerchandiseHelper.filterBlank(values).stream()
                .map(String::trim)
                .map(Long::valueOf)
                .toList();
        if (parsedValues.isEmpty()) {
            return null;
        }

        return (root, query, cb) -> resolvePath(root, path).in(parsedValues);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Specification<Product> greaterThanOrEqualTo(String path, Comparable<?> value) {
        if (value == null) {
            return null;
        }

        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                (jakarta.persistence.criteria.Expression) resolvePath(root, path),
                (Comparable) value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Specification<Product> lessThanOrEqualTo(String path, Comparable<?> value) {
        if (value == null) {
            return null;
        }

        return (root, query, cb) -> cb.lessThanOrEqualTo(
                (jakarta.persistence.criteria.Expression) resolvePath(root, path),
                (Comparable) value);
    }

    @SuppressWarnings("unchecked")
    private <T> jakarta.persistence.criteria.Path<T> resolvePath(jakarta.persistence.criteria.From<?, ?> from, String path) {
        jakarta.persistence.criteria.Path<?> current = from;
        for (String segment : path.split("\\.")) {
            current = current.get(segment);
        }
        return (jakarta.persistence.criteria.Path<T>) current;
    }

}
