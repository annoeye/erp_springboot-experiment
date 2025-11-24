package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.AttributesService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.CategoryService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.ProductService;
import com.anno.ERP_SpringBoot_Experiment.service.MinioService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.PageableData;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.web.rest.MerchandiseController;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class merchandiseControllerImpl implements MerchandiseController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final AttributesService attributesService;
    private final MinioService minioService;

    /*************       Product CRUD      *****************/

    @Override
    public Response<?> addProduct(@ModelAttribute CreateProductRequest request) {
        return productService.addProduct(request);
    }

    @Override
    public Response<ProductDto> updateProduct(@RequestBody UpdateProductRequest request) {
        return productService.updateProduct(request);
    }

    @Override
    public Response<?> deleteProduct(@RequestParam List<String> ids) {
        // Chuẩn hóa tất cả UUID strings
        List<UUID> uuidList = ids.stream()
                .map(id -> {
                    try {
                        return UUID.fromString(id);
                    } catch (IllegalArgumentException e) {
                        String normalized = normalizeUUID(id);
                        return UUID.fromString(normalized);
                    }
                })
                .collect(Collectors.toList());
        
        return productService.deleteProduct(uuidList);
    }

    private String normalizeUUID(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("UUID không được để trống");
        }
        
        String cleanUuid = uuid.replaceAll("-", "");
        
        if (cleanUuid.length() != 32) {
            throw new IllegalArgumentException("UUID phải có 32 ký tự");
        }
        
        return String.format("%s-%s-%s-%s-%s",
                cleanUuid.substring(0, 8),
                cleanUuid.substring(8, 12),
                cleanUuid.substring(12, 16),
                cleanUuid.substring(16, 20),
                cleanUuid.substring(20, 32)
        ).toLowerCase();
    }

    @Override
    public Page<ProductDto> searchProduct(@RequestBody ProductSearchRequest request) {
        return productService.search(request);
    }

    /*************       Product Images Management      *****************/

    @Override
    public Response<?> addProductImages(
            @PathVariable String productId,
            @RequestParam("images") List<MultipartFile> images) {
        return productService.addProductImages(productId, images);
    }

    @Override
    public Response<?> deleteProductImage(
            @PathVariable String productId,
            @RequestParam String imageKey) {
        return productService.deleteProductImage(productId, imageKey);
    }

    @Override
    public Response<?> replaceProductImages(
            @PathVariable String productId,
            @RequestParam("images") List<MultipartFile> images) {
        return productService.replaceProductImages(productId, images);
    }

    /*************       Category CRUD      *****************/

    @Override
    public Response<String> addCategory(@RequestParam String name) {
        return categoryService.create(name);
    }

    @Override
    public Response<CategoryDto> updateCategory(@RequestBody CategoryDto categoryDto) {
        return categoryService.update(categoryDto);
    }

    @Override
    public Response<?> deleteCategory(@RequestParam List<String> ids) {
        return categoryService.delete(ids);
    }

    @Override
    public Response<PagingResponse<CategoryDto>> searchCategory(@RequestBody CategorySearchRequest request) {
        final Page<CategoryDto> categories = categoryService.search(request);
        final PagingRequest page = request.getPaging();
        
        return Response.ok(
                PagingResponse.<CategoryDto>builder()
                        .contents(categories.getContent())
                        .paging(new PageableData()
                                .setPageNumber(page.getPage() - 1)
                                .setTotalPage(categories.getTotalPages())
                                .setPageSize(page.getSize())
                                .setTotalRecord(categories.getTotalElements())
                        )
                        .build()
        );
    }

    /*************       Attributes Management      *****************/

    @Override
    public Response<AttributesDto> addAttributes(@RequestBody CreateAttributesRequest request) {
        return attributesService.create(request);
    }

    @Override
    public Response<AttributesDto> updateAttributes(@RequestBody UpdateAttributesRequest request) {
        return attributesService.update(request);
    }

    @Override
    public Response<?> deleteAttributes(@RequestParam List<String> skus) {
        return attributesService.delete(skus);
    }

    @Override
    public Response<?> deleteAttributesByProduct(@PathVariable String productId) {
        return attributesService.deleteByProduct(productId);
    }

    @Override
    public Response<List<AttributesDto>> getAttributesByProduct(@PathVariable String productId) {
        return attributesService.getByProduct(productId);
    }

    @Override
    public Response<AttributesDto> getAttributesBySku(@PathVariable String sku) {
        return attributesService.getBySku(sku);
    }

    public ResponseEntity<String> upload(@RequestBody MultipartFile file) throws IOException {
        return ResponseEntity.ok(minioService.uploadFile(file));
    }
}
