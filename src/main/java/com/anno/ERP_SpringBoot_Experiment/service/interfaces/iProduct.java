package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ProductIsExiting;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import lombok.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface iProduct {
    Response<?> addProduct(CreateProductRequest request);
    Response<?> updateProduct(UpdateProductRequest request);
    Response<?> deleteProduct(@NonNull final List<UUID> ids);
//    Page<ProductDto> getProduct(@NonNull final GetProductRequest request);
    ProductIsExiting isExiting(String name);

    Response<?> addProductImages(String productId, List<MultipartFile> images);
    Response<?> deleteProductImage(String productId, String imageKey);
    Response<?> replaceProductImages(String productId, List<MultipartFile> images);
}
