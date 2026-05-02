package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.AttributesService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.CategoryService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.ProductService;
import com.anno.ERP_SpringBoot_Experiment.service.MinioService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.CategoryExitingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ProductIsExiting;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.PageableData;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.web.rest.MerchandiseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Merchandise Management", description = "API quản lý sản phẩm, danh mục và thuộc tính")
public class merchandiseControllerImpl implements MerchandiseController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final AttributesService attributesService;
    private final MinioService minioService;

    /************* Product CRUD *****************/

    @Override
    @Operation(summary = "Tạo sản phẩm mới", description = "Tạo một sản phẩm mới trong hệ thống")
    public Response<?> addProduct(@RequestBody CreateProductRequest request) {
        return productService.addProduct(request);
    }

    @Override
    @Operation(summary = "Cập nhật sản phẩm", description = "Cập nhật thông tin của một sản phẩm đã tồn tại")
    public Response<?> updateProduct(@Valid @RequestBody UpdateProductRequest request) {
        return productService.updateProduct(request);
    }

    @Override
    @Operation(summary = "Xóa sản phẩm", description = "Xóa một hoặc nhiều sản phẩm theo danh sách IDs")
    public Response<?> deleteProduct(@RequestParam List<String> ids) {
        List<Long> longList = ids.stream()
                .map(id -> {
                    try {
                        return Long.valueOf(id.trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("ID phải là một số nguyên hợp lệ.");
                    }
                }).collect(Collectors.toList());
        return productService.deleteProduct(longList);
    }

     @Override
     @Operation(summary = "Tìm kiếm sản phẩm", description = "Tìm kiếm sản phẩm theo các tiêu chí như tên, danh mục, giá, v.v.")
     public Page<ProductDto> searchProduct(@RequestBody GetProductRequest request)
     { return productService.searchProducts(request); }

    /************* Product Images Management *****************/

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
    @Operation(summary = "Xem ảnh", description = "Lấy và xem trực tiếp ảnh sản phẩm dựa trên tên file ảnh")
    public byte[] viewProductImage(@PathVariable String imageName) {
        return productService.getProductImage(imageName);
    }

    @Override
    public ProductIsExiting checkProduct(String name) {
        return productService.isExiting(name);
    }

    @Override
    @Operation(summary = "Tăng lượt xem sản phẩm", description = "Tăng lượt xem khi người dùng truy cập chi tiết sản phẩm")
    public Response<?> incrementViewCount(@PathVariable String productId) {
        productService.viewCount(productId);
        return Response.ok("Đã tăng lượt xem");
    }

    /************* Category CRUD *****************/

    @Override
    @Operation(summary = "Tạo danh mục mới", description = "Tạo một danh mục sản phẩm mới")
    public Response<?> addCategory(@RequestParam String name) {
        return categoryService.create(name);
    }

    @Override
    @Operation(summary = "Cập nhật danh mục", description = "Cập nhật thông tin của một danh mục đã tồn tại")
    public Response<?> updateCategory(@RequestBody UpdateCategoryRequest categoryDto) {
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
                        .paging(PageableData.from(categories))
                        .build());
    }

    @Override
    public CategoryExitingResponse check(String name) {
        return categoryService.isExiting(name);
    }

    /************* Attributes Management *****************/

    @Override
    @Operation(summary = "Tạo thuộc tính sản phẩm", description = "Tạo thuộc tính/biến thể mới cho sản phẩm (ví dụ: màu sắc, kích thước)")
    public Response<List<AttributesDto>> addAttributes(@RequestBody CreateAttributesRequest request) {
        return attributesService.create(request);
    }

    @Override
    @Operation(summary = "Cập nhật thuộc tính sản phẩm", description = "Cập nhật thông tin của thuộc tính/biến thể sản phẩm")
    public Response<?> updateAttributes(@RequestBody UpdateAttributesRequest request) {
        return attributesService.update(request);
    }

    @Override
    @Operation(summary = "Xóa thuộc tính theo SKU", description = "Xóa một hoặc nhiều thuộc tính sản phẩm theo danh sách ids")
    public Response<?> deleteAttributes(@RequestParam List<String> ids) {
        return attributesService.delete(ids);
    }

    @Override
    @Operation(summary = "Xóa tất cả thuộc tính của sản phẩm", description = "Xóa tất cả các thuộc tính/biến thể của một sản phẩm cụ thể")
    public Response<?> deleteAttributesByProduct(@PathVariable String productId) {
        return attributesService.deleteByProduct(productId);
    }

    @Override
    @Operation(summary = "Tìm kiếm thuộc tính sản phẩm", description = "Tìm kiếm thuộc tính/biến thể theo các tiêu chí với phân trang")
    public Response<PagingResponse<AttributesDto>> searchAttributes(@RequestBody AttributesSearchRequest request) {
        final Page<AttributesDto> attributes = attributesService.search(request);
        return Response.ok(
                PagingResponse.<AttributesDto>builder()
                        .contents(attributes.getContent())
                        .paging(PageableData.from(attributes))
                        .build());
    }

    @Operation(summary = "Upload file", description = "Upload file lên MinIO storage")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(minioService.uploadFile(file));
    }
}
