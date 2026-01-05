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
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.*;
import com.anno.ERP_SpringBoot_Experiment.web.rest.MerchandiseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Merchandise Management", description = "API quản lý sản phẩm, danh mục và thuộc tính")
public class merchandiseControllerImpl implements MerchandiseController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final AttributesService attributesService;
    private final MinioService minioService;

    /*************       Product CRUD      *****************/

    @Override
    @Operation(summary = "Tạo sản phẩm mới", description = "Tạo một sản phẩm mới trong hệ thống")
    public Response<?> addProduct(@RequestBody CreateProductRequest request) {
        return productService.addProduct(request);
    }

    @Override
    @Operation(summary = "Cập nhật sản phẩm", description = "Cập nhật thông tin của một sản phẩm đã tồn tại")
    public Response<ProductDto> updateProduct(@RequestBody UpdateProductRequest request) {
        return productService.updateProduct(request);
    }

    @Override
    @Operation(summary = "Xóa sản phẩm", description = "Xóa một hoặc nhiều sản phẩm theo danh sách IDs")
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
    @Operation(summary = "Tìm kiếm sản phẩm", description = "Tìm kiếm sản phẩm theo các tiêu chí như tên, danh mục, giá, v.v.")
    public Page<ProductDto> searchProduct(@RequestBody ProductSearchRequest request) {
        return productService.search(request);
    }

    /*************       Product Images Management      *****************/

    @Override
    @Operation(summary = "Thêm hình ảnh sản phẩm", description = "Thêm một hoặc nhiều hình ảnh cho sản phẩm")
    public Response<?> addProductImages(
            @PathVariable String productId,
            @RequestParam("images") List<MultipartFile> images) {
        return productService.addProductImages(productId, images);
    }

    @Override
    @Operation(summary = "Xóa hình ảnh sản phẩm", description = "Xóa một hình ảnh cụ thể của sản phẩm")
    public Response<?> deleteProductImage(
            @PathVariable String productId,
            @RequestParam String imageKey) {
        return productService.deleteProductImage(productId, imageKey);
    }

    @Override
    @Operation(summary = "Thay thế hình ảnh sản phẩm", description = "Thay thế toàn bộ hình ảnh hiện tại của sản phẩm bằng hình ảnh mới")
    public Response<?> replaceProductImages(
            @PathVariable String productId,
            @RequestParam("images") List<MultipartFile> images) {
        return productService.replaceProductImages(productId, images);
    }

    @Override
    public ProductIsExiting checkProduct(String name) {
        return productService.isExiting(name);
    }

    /*************       Category CRUD      *****************/

    @Override
    @Operation(summary = "Tạo danh mục mới", description = "Tạo một danh mục sản phẩm mới")
    public Response<String> addCategory(@RequestParam String name) {
        return categoryService.create(name);
    }

    @Override
    @Operation(summary = "Cập nhật danh mục", description = "Cập nhật thông tin của một danh mục đã tồn tại")
    public Response<CategoryDto> updateCategory(@RequestBody CategoryDto categoryDto) {
        return categoryService.update(categoryDto);
    }

    @Override
    @Operation(summary = "Xóa danh mục", description = "Xóa một hoặc nhiều danh mục theo danh sách IDs")
    public Response<?> deleteCategory(@RequestParam List<String> ids) {
        return categoryService.delete(ids);
    }

    @Override
    @Operation(summary = "Tìm kiếm danh mục", description = "Tìm kiếm danh mục theo các tiêu chí với phân trang")
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

    @Override
    public CategoryExitingResponse check(String name) {
        return categoryService.isExiting(name);
    }

    /*************       Attributes Management      *****************/

    @Override
    @Operation(summary = "Tạo thuộc tính sản phẩm", description = "Tạo thuộc tính/biến thể mới cho sản phẩm (ví dụ: màu sắc, kích thước)")
    public Response<AttributesDto> addAttributes(@RequestBody CreateAttributesRequest request) {
        return attributesService.create(request);
    }

    @Override
    @Operation(summary = "Cập nhật thuộc tính sản phẩm", description = "Cập nhật thông tin của thuộc tính/biến thể sản phẩm")
    public Response<AttributesDto> updateAttributes(@RequestBody UpdateAttributesRequest request) {
        return attributesService.update(request);
    }

    @Override
    @Operation(summary = "Xóa thuộc tính theo SKU", description = "Xóa một hoặc nhiều thuộc tính sản phẩm theo danh sách SKUs")
    public Response<?> deleteAttributes(@RequestParam List<String> skus) {
        return attributesService.delete(skus);
    }

    @Override
    @Operation(summary = "Xóa tất cả thuộc tính của sản phẩm", description = "Xóa tất cả các thuộc tính/biến thể của một sản phẩm cụ thể")
    public Response<?> deleteAttributesByProduct(@PathVariable String productId) {
        return attributesService.deleteByProduct(productId);
    }

    @Override
    @Operation(summary = "Lấy thuộc tính theo sản phẩm", description = "Lấy danh sách tất cả thuộc tính/biến thể của một sản phẩm")
    public Response<List<AttributesDto>> getAttributesByProduct(@PathVariable String productId) {
        return attributesService.getByProduct(productId);
    }

    @Override
    @Operation(summary = "Lấy thuộc tính theo SKU", description = "Lấy thông tin chi tiết của một thuộc tính/biến thể theo SKU")
    public Response<AttributesDto> getAttributesBySku(@PathVariable String sku) {
        return attributesService.getBySku(sku);
    }

    @Operation(summary = "Upload file", description = "Upload file lên MinIO storage")
    public ResponseEntity<String> upload(@RequestBody MultipartFile file) throws IOException {
        return ResponseEntity.ok(minioService.uploadFile(file));
    }
}
