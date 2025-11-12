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
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iProduct;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Override
    public Response<?> addProduct(@ModelAttribute CreateProductRequest request) {
        List<MultipartFile> images = request.getImages();

        Optional<Category> optionalCategory = categoryRepository.findCategoriesBySkuInfo_SKU(request.getCategorySku());
        Category category = optionalCategory.orElseThrow(
                () -> new BusinessException("Danh mục không hợp lệ.")
        );

        AuditInfo audit = new AuditInfo();
        audit.setCreatedAt(LocalDateTime.now());
        audit.setCreatedBy(securityUtil.getCurrentUsername());

        List<MediaItem> mediaItems = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;

                try {
                    String url = minioService.uploadFile(file);
                    String key = featureMerchandiseHelper.generateKey();

                    MediaItem mediaItem = new MediaItem();
                    mediaItem.setKey(key);
                    mediaItem.setUrl(url);
                    mediaItems.add(mediaItem);
                } catch (Exception e) {
                    throw new BusinessException("Lỗi khi upload file: " + e.getMessage());
                }
            }
        }

        Product product = Product.builder()
                .name(request.getName().replaceAll("\\s+", "-").toUpperCase())
                .category(category)
                .skuInfo(new SkuInfo())
                .auditInfo(audit)
                .description(request.getDescription())
                .mediaItems(mediaItems)
                .status(request.getStatus())
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
    // Thiếu RUD

    @Override
    public Response<?> deleteProduct(@NonNull final List<UUID> ids) {
        // xóa 1 list
        productRepository.softDeleteAllByIds(ids, securityUtil.getCurrentUsername());
        return Response.noContent();
    }

    @Override
    @Transactional
    public Response<?> addProductImages(String productId, List<MultipartFile> images) {
        final var product = productRepository.findById(UUID.fromString(productId))
                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại."));

        if (images == null || images.isEmpty()) {
            throw new BusinessException("Ảnh không được để trống.");
        }

        List<MediaItem> newItems = new ArrayList<>();
        for (MultipartFile file : images) {
            if (newItems.isEmpty()) continue;

            try {
                String url = minioService.uploadFile(file);
                String key = featureMerchandiseHelper.generateKey();

                MediaItem mediaItem = new MediaItem();
                mediaItem.setKey(key);
                mediaItem.setUrl(url);
                newItems.add(mediaItem);
            } catch (Exception e) {
                throw new BusinessException("Lỗi khi upload file: " + e.getMessage());
            }
        }

        product.getMediaItems().addAll(newItems);
        log.info("Đã thêm {} ảnh mới vào sản phẩm {}", newItems.size(), productId);

        return Response.ok(productRepository.save(product), "Thêm ảnh thành công.");
    }

    @Override
    @Transactional
    public Response<?> deleteProductImage(String productId, String imageKey) {
        final var product = productRepository.findById(UUID.fromString(productId))
                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại."));

        boolean removed = product.getMediaItems().removeIf(mediaItem -> mediaItem.getKey().equals(imageKey));

        if (!removed) throw new BusinessException("Không tìm thấy ảnh với key: " + imageKey);

        log.info("Đã xóa ảnh {} khỏi sản phẩm {}", imageKey, productId);
        return Response.ok(productRepository.save(product), "Xóa ảnh thành công.");

    }

    @Override
    @Transactional
    public Response<?> replaceProductImages(String productId, List<MultipartFile> images) {
        return null;
    }

}
