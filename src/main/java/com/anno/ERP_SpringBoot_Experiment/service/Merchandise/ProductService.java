package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.ProductMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.MediaItem;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.MinioService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ProductIsExiting;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iProduct;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                MediaItem mediaItem = new MediaItem();
                mediaItem.setKey(key);
                mediaItem.setUrl(url);
                mediaItems.add(mediaItem);
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
            throw new BusinessException("Lỗi khi upload file: " + e.getMessage());
        }
    }

    @Override
    public Response<?> addProduct(CreateProductRequest request) {
        Category category = categoryRepository
                .findCategoryById(featureMerchandiseHelper.convertStringToUUID(request.getCategoryId()))
                .orElseThrow(() -> new BusinessException("Danh mục không tồn tại."));

        AuditInfo audit = new AuditInfo();
        audit.setCreatedAt(LocalDateTime.now());
        audit.setCreatedBy(securityUtil.getCurrentUsername());
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSku(category.getName().toUpperCase());

        Product product = Product.builder()
                .name(request.getName())
                .category(category)
                .skuInfo(skuInfo)
                .auditInfo(audit)
                .build();

        return Response.ok(productRepository.save(product));
    }

    @Override
    @Transactional
    public Response<ProductDto> updateProduct(UpdateProductRequest request) {
        if (request.getId() == null || request.getId().isBlank()) {
            throw new BusinessException("Sản phẩm không không được để trống.");
        }

        final var product = productRepository.findById(UUID.fromString(request.getId()))
                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại."));

        if (request.getCategorySku() != null && !request.getCategorySku().isBlank()) {
            Category category = categoryRepository.findCategoriesBySkuInfo_SKU(request.getCategorySku())
                    .orElseThrow(() -> new BusinessException("Danh mục không tồn tại."));
            product.setCategory(category);
        }

        product.getAuditInfo().setUpdatedAt(LocalDateTime.now());
        product.getAuditInfo().setUpdatedBy(securityUtil.getCurrentUsername());

        productMapper.updateFromRequest(request, product);

        log.info("Đã cập nhật sản phẩm '{}' với ID {}", request.getName(), request.getId());
        return Response.ok(productMapper.toDto(productRepository.save(product)), "Cập nhập sản phẩm thành công.");
    }

    @Override
    public Response<?> deleteProduct(@NonNull final List<UUID> ids) {
        // xóa 1 list
        productRepository.softDeleteAllByIds(ids, securityUtil.getCurrentUsername());
        return Response.noContent();
    }

    @Override
    @Transactional
    public Page<ProductDto> search(@NonNull ProductSearchRequest request) {
        // productRepository.deleteAllExpiredCategories();
        return productRepository.findAll(request.specification(), request.getPaging().pageable())
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
    @Transactional
    public Response<?> addProductImages(String productId, List<MultipartFile> images) {
        final var product = productRepository.findById(featureMerchandiseHelper.convertStringToUUID(productId))
                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại."));

        if (images == null || images.isEmpty()) {
            throw new BusinessException("Ảnh không được để trống.");
        }

        List<MediaItem> newItems = uploadImages(images);
        product.getMediaItems().addAll(newItems);
        log.info("Đã thêm {} ảnh mới vào sản phẩm {}", newItems.size(), productId);

        return Response.ok(productRepository.save(product), "Thêm ảnh thành công.");
    }

    @Override
    @Transactional
    public Response<?> deleteProductImage(String productId, String imageKey) {
        final var product = productRepository.findById(featureMerchandiseHelper.convertStringToUUID(productId))
                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại."));

        MediaItem itemToDelete = product.getMediaItems().stream()
                .filter(mediaItem -> mediaItem.getKey().equals(imageKey))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Không tìm thấy ảnh với key: " + imageKey));

        try {
            minioService.deleteFile(itemToDelete.getUrl());
        } catch (Exception e) {
            log.error("Lỗi khi xóa file trên MinIO: {}", e.getMessage());
        }

        product.getMediaItems().remove(itemToDelete);

        log.info("Đã xóa ảnh {} khỏi sản phẩm {}", imageKey, productId);

        Product savedProduct = productRepository.save(product);
        return Response.ok(productMapper.toDto(savedProduct), "Xóa ảnh thành công.");
    }

    @Override
    @Transactional
    public Response<?> replaceProductImages(String productId, List<MultipartFile> images) {
        UUID uuid;
        try {
            uuid = UUID.fromString(featureMerchandiseHelper.normalizeUUID(productId));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("ID sản phẩm không hợp lệ.");
        }

        final var product = productRepository.findById(uuid)
                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại."));

        if (images == null || images.isEmpty()) {
            throw new BusinessException("Ảnh không được để trống.");
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
        log.info("Đã thay thế {} ảnh cho sản phẩm {}", newItems.size(), productId);

        // Trả về DTO thay vì Entity
        Product savedProduct = productRepository.save(product);
        return Response.ok(productMapper.toDto(savedProduct), "Thay thế ảnh thành công.");
    }

}
