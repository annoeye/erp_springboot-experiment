package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.MediaItem;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.MinioService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iProduct;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
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
    // Thiếu RUD

    @Override
    public Response<?> deleteProduct(@NonNull final List<UUID> ids) {
        // xóa 1 list
        productRepository.softDeleteAllByIds(ids, securityUtil.getCurrentUsername());
        return Response.noContent();
    }

}
