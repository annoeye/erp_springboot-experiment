package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.AttributesService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.CategoryService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.ProductService;
import com.anno.ERP_SpringBoot_Experiment.service.MinioService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CategorySearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.PagingRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.PageableData;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.web.rest.MerchandiseController;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class merchandiseControllerImpl implements MerchandiseController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final AttributesService attributesService;
    private final MinioService minioService;

    @Override
    public Response<?> addProduct(@ModelAttribute CreateProductRequest request) {
        return productService.addProduct(request);
    }

    @Override
    public Response<CategoryDto> updateCategory(final CategoryDto categoryDto) {
        return categoryService.update(categoryDto);
    }

    @Override
    public Response<String> addCategory(final String name) {
        categoryService.create(name);
       return Response.ok(name);
    }

    @Override
    public Response<?> deleteCategory(@RequestParam final List<String> ids) {
        return categoryService.delete(ids);
    }

    @Override
    public Response<PagingResponse<CategoryDto>> searchCategory(CategorySearchRequest request) {
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


    public ResponseEntity<String> upload(@RequestBody MultipartFile file) throws IOException {
        return ResponseEntity.ok(minioService.uploadFile(file));
    }

    @Override
    public Response<AttributesDto> addAttributes(CreateAttributesRequest request) {
        return attributesService.create(request);
    }
}
