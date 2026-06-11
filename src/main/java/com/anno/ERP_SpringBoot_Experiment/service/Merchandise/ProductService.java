package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.ProductMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.MediaItem;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SearchCriteria;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SpecificationBuilder;
import com.anno.ERP_SpringBoot_Experiment.service.MinioService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateProductRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.GetProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ProductIsExiting;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iProduct;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final com.anno.ERP_SpringBoot_Experiment.service.CacheSyncService cacheSyncService;
    private final org.springframework.cache.CacheManager cacheManager;
    private final jakarta.persistence.EntityManager entityManager;

    private List<MediaItem> uploadImages(List<MultipartFile> images) {
        List<MediaItem> mediaItems = new ArrayList<>();
        List<String> uploadedUrls = new ArrayList<>();

        try {
            for (MultipartFile file : images) {
                if (file.isEmpty())
                    continue;

                String url = minioService.uploadFile(file);
                uploadedUrls.add(url);

                String key = featureMerchandiseHelper.generateKey();
                mediaItems.add(MediaItem.builder()
                        .key(key)
                        .url(url)
                        .build());
            }
            return mediaItems;

        } catch (Exception e) {
            // Rollback
            for (String url : uploadedUrls) {
                try {
                    minioService.deleteFile(url);
                } catch (Exception deleteEx) {
                    log.error("Không thể xóa file {} sau khi rollback: {}", url, deleteEx.getMessage());
                }
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Lỗi khi upload file: " + e.getMessage());
        }
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
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
    @CacheEvict(value = "products", allEntries = true)
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

        // Hook: Báo hiệu cho luồng đồng bộ chạy ngầm cập nhật cache RAM
        cacheSyncService.markProductDirty(product.getId());

        return Response.ok("Cập nhật sản phẩm thành công.");
    }

    @Override
    @CacheEvict(value = "productDetails", allEntries = true)
    public Response<?> deleteProduct(@NonNull final List<Long> ids) {
        // Xóa mềm danh sách sản phẩm
        productRepository.softDeleteAllByIds(ids, securityUtil.getCurrentUsername());
        return Response.noContent();
    }

    private List<SearchCriteria> buildProductSearchCriteria(GetProductRequest request) {
        List<SearchCriteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(request.getKeyword())) {
            // Default "keyword" to search within name
            criteriaList.add(new SearchCriteria("name", "~", request.getKeyword()));
        }

        if (StringUtils.hasText(request.getCreatedBy())) {
            criteriaList.add(new SearchCriteria("auditInfo.createdBy", "~", request.getCreatedBy()));
        }

        if (!CollectionUtils.isEmpty(request.getProductIds())) {
            List<Long> uuidList = request.getProductIds().stream()
                    .map(Long::valueOf)
                    .toList();
            criteriaList.add(new SearchCriteria("id", "~", uuidList));
        }

        if (!CollectionUtils.isEmpty(request.getStatuses())) {
            criteriaList.add(new SearchCriteria("status", "~", request.getStatuses()));
        }

        if (!CollectionUtils.isEmpty(request.getCategoryIds())) {
            List<Long> uuidList = request.getCategoryIds().stream()
                    .map(Long::valueOf)
                    .toList();
            criteriaList.add(new SearchCriteria("category.id", "~", uuidList));
        }

        if (StringUtils.hasText(request.getCategoryId())) {
            criteriaList.add(new SearchCriteria("category.id", ":", Long.valueOf(request.getCategoryId().trim())));
        }

        if (request.getMinSoldQuantity() != null) {
            criteriaList.add(new SearchCriteria("totalSoldQuantity", ">", request.getMinSoldQuantity()));
        }
        if (request.getMaxSoldQuantity() != null) {
            criteriaList.add(new SearchCriteria("totalSoldQuantity", "<", request.getMaxSoldQuantity()));
        }

        if (request.getMinRevenue() != null) {
            criteriaList.add(new SearchCriteria("totalRevenue", ">", request.getMinRevenue()));
        }
        if (request.getMaxRevenue() != null) {
            criteriaList.add(new SearchCriteria("totalRevenue", "<", request.getMaxRevenue()));
        }

        if (request.getMinOrders() != null) {
            criteriaList.add(new SearchCriteria("totalOrders", ">", request.getMinOrders()));
        }
        if (request.getMaxOrders() != null) {
            criteriaList.add(new SearchCriteria("totalOrders", "<", request.getMaxOrders()));
        }

        if (request.getMinView() != null) {
            criteriaList.add(new SearchCriteria("viewCount", ">", request.getMinView()));
        }
        if (request.getMinRating() != null) {
            criteriaList.add(new SearchCriteria("averageRating", ">", request.getMinRating()));
        }
        if (request.getMinReviews() != null) {
            criteriaList.add(new SearchCriteria("reviewCount", ">", request.getMinReviews()));
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
    @Transactional
    @Cacheable(value = "products", key = "#request.hashCode()")
    public Page<ProductDto> searchProducts(@NonNull GetProductRequest request) {
        List<Long> ids = searchProductIds(request);
        List<ProductDto> content = getProductsByIds(ids).getData();

        List<SearchCriteria> criteriaList = buildProductSearchCriteria(request);
        SpecificationBuilder<Product> builder = new SpecificationBuilder<>(criteriaList);
        Specification<Product> spec = builder.build();

        Pageable pageable = (request.getPaging() != null) ? request.getPaging().pageable() : PageRequest.of(0, 10);

        long total;
        if (spec != null) {
            total = productRepository.count(spec);
        } else {
            total = productRepository.count();
        }

        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional
    public List<Long> searchProductIds(@NonNull GetProductRequest request) {
        List<SearchCriteria> criteriaList = buildProductSearchCriteria(request);
        SpecificationBuilder<Product> builder = new SpecificationBuilder<>(criteriaList);
        Specification<Product> spec = builder.build();

        jakarta.persistence.criteria.CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        jakarta.persistence.criteria.CriteriaQuery<Long> query = cb.createQuery(Long.class);
        jakarta.persistence.criteria.Root<Product> root = query.from(Product.class);

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
    @Transactional
    public Response<?> addProductImages(String productId, List<MultipartFile> images) {
        final var product = productRepository.findById(Long.valueOf(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        if (CollectionUtils.isEmpty(images)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Ảnh không được để trống.");
        }

        List<MediaItem> newItems = uploadImages(images);
        product.getMediaItems().addAll(newItems);
        product.addUpdateEntry("Thêm " + newItems.size() + " ảnh sản phẩm",
                securityUtil.getCurrentUsername());
        log.info("Đã thêm {} ảnh mới vào sản phẩm {}", newItems.size(), productId);

        return Response.ok(productRepository.save(product), "Thêm ảnh thành công.");
    }

    @Override
    @Transactional
    public Response<?> deleteProductImage(String productId, String imageKey) {
        final var product = productRepository.findById(Long.valueOf(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        MediaItem itemToDelete = product.getMediaItems().stream()
                .filter(mediaItem -> mediaItem.getKey().equals(imageKey))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy ảnh với key: " + imageKey));

        try {
            minioService.deleteFile(itemToDelete.getUrl());
        } catch (Exception e) {
            log.error("Lỗi khi xóa file trên MinIO: {}", e.getMessage());
        }

        product.getMediaItems().remove(itemToDelete);
        product.addUpdateEntry("Xóa ảnh sản phẩm: " + imageKey, securityUtil.getCurrentUsername());

        log.info("Đã xóa ảnh {} khỏi sản phẩm {}", imageKey, productId);

        Product savedProduct = productRepository.save(product);
        return Response.ok(productMapper.toDto(savedProduct), "Xóa ảnh thành công.");
    }

    @Override
    @Transactional
    public Response<?> replaceProductImages(String productId, List<MultipartFile> images) {
        Long uuid;
        try {
            uuid = Long.valueOf(productId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "ID sản phẩm không hợp lệ.");
        }

        final var product = productRepository.findById(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        if (CollectionUtils.isEmpty(images)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Ảnh không được để trống.");
        }

        for (MediaItem oldItem : product.getMediaItems()) {
            try {
                minioService.deleteFile(oldItem.getUrl());
            } catch (Exception e) {
                log.error("Lỗi khi xóa file cũ {}: {}", oldItem.getUrl(), e.getMessage());
            }
        }

        product.getMediaItems().clear();

        List<MediaItem> newItems = uploadImages(images);
        product.getMediaItems().addAll(newItems);
        product.addUpdateEntry("Thay thế " + newItems.size() + " ảnh sản phẩm",
                securityUtil.getCurrentUsername());
        log.info("Đã thay thế {} ảnh cho sản phẩm {}", newItems.size(), productId);

        // Trả về DTO thay vì Entity
        Product savedProduct = productRepository.save(product);
        return Response.ok(productMapper.toDto(savedProduct), "Thay thế ảnh thành công.");
    }

    @Override
    public byte[] getProductImage(String imageName) {
        try (java.io.InputStream inputStream = minioService.getFile(imageName)) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("Lỗi khi đọc ảnh {}: {}", imageName, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Không thể đọc dữ liệu ảnh: " + imageName);
        }
    }

    @Override
    @Transactional
    public Response<List<ProductDto>> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Response.ok(new java.util.ArrayList<>());
        }

        org.springframework.cache.Cache cache = cacheManager.getCache("productDetails");
        java.util.Map<Long, ProductDto> dtoMap = new java.util.HashMap<>();
        List<Long> missingIds = new java.util.ArrayList<>();

        // 1. Check RAM cache first
        for (Long id : ids) {
            ProductDto cached = cache != null ? cache.get(id, ProductDto.class) : null;
            if (cached != null) {
                dtoMap.put(id, cached);
            } else {
                missingIds.add(id);
            }
        }

        // 2. Fetch missing from DB
        if (!missingIds.isEmpty()) {
            List<Product> products = productRepository.findAllById(missingIds);
            for (Product p : products) {
                ProductDto dto = productMapper.toDto(p);
                if (cache != null) {
                    cache.put(p.getId(), dto);
                }
                dtoMap.put(p.getId(), dto);
            }
        }

        // 3. Reconstruct list preserving the original requested order
        List<ProductDto> result = new java.util.ArrayList<>();
        for (Long id : ids) {
            ProductDto dto = dtoMap.get(id);
            if (dto != null) {
                result.add(dto);
            }
        }

        return Response.ok(result);
    }
}
