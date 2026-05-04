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
                                        .replaceFirst("-", "-" + request.getCategorySku().substring(request.getCategorySku().length() - 2)))
                                .build())
                        .auditInfo(AuditInfo.builder()
                                .createdAt(LocalDateTime.now())
                                .createdBy(securityUtil.getCurrentUsername())
                                .build())
                        .status(Stream.of(ActiveStatus.ACTIVE, ActiveStatus.LOCKED)
                                .filter(s -> s.name().equals(request.getStatus()))
                                .findFirst()
                                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                                        "Định dạng không hợp lệ!")))
                        .build());
        return Response.ok("Thêm sản phẩm '" +request.getName() + "' thành công.");
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

        if (product.getAuditInfo() == null) {
            product.setAuditInfo(new AuditInfo());
        }
        product.getAuditInfo().addUpdateEntry("Cập nhật thông tin sản phẩm", securityUtil.getCurrentUsername());

        log.info("Đã cập nhật sản phẩm '{}' với ID {}", product.getName(), product.getId());
        productRepository.save(product);
        return Response.ok("Cập nhật sản phẩm thành công.");
    }

    @Override
    public Response<?> deleteProduct(@NonNull final List<Long> ids) {
        // xóa 1 list
        productRepository.softDeleteAllByIds(ids, securityUtil.getCurrentUsername());
        return Response.noContent();
    }

    @Override
    @Transactional
    public Page<ProductDto> searchProducts(@NonNull GetProductRequest request) {
        List<SearchCriteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(request.getKeyword())) {
            // Default "keyword" to search within name
            criteriaList.add(new SearchCriteria("name", ":", request.getKeyword()));
        }

        if (!CollectionUtils.isEmpty(request.getCreatedBy())) {
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

        SpecificationBuilder<Product> builder = new SpecificationBuilder<>(criteriaList);
        Specification<Product> spec = builder.build();

        Pageable pageable = (request.getPaging() != null) ? request.getPaging().pageable() : PageRequest.of(0, 10);

        return productRepository.findAll(spec, pageable)
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
        product.getAuditInfo().addUpdateEntry("Thêm " + newItems.size() + " ảnh sản phẩm", securityUtil.getCurrentUsername());
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
        product.getAuditInfo().addUpdateEntry("Xóa ảnh sản phẩm: " + imageKey, securityUtil.getCurrentUsername());

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
        product.getAuditInfo().addUpdateEntry("Thay thế " + newItems.size() + " ảnh sản phẩm", securityUtil.getCurrentUsername());
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

}
