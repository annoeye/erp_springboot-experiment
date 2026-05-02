package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.request.GetProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ProductIsExiting;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface iProduct {
    Response<?> addProduct(CreateProductRequest request);
    Response<?> updateProduct(UpdateProductRequest request);
    Response<?> deleteProduct(@NonNull final List<Long> ids);
    Page<ProductDto> searchProducts(@NonNull final GetProductRequest request);
    ProductIsExiting isExiting(String name);
    void viewCount(String productId);
    void totalSoldQuantity(String productId);
    void totalRevenue(String productId, double price);
    Response<?> addProductImages(String productId, List<MultipartFile> images);
    Response<?> deleteProductImage(String productId, String imageKey);
    Response<?> replaceProductImages(String productId, List<MultipartFile> images);
    byte[] getProductImage(String imageName);
}
