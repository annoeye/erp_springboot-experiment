
package com.anno.ERP_SpringBoot_Experiment.web.rest;

import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/api/merchandise")
public interface MerchandiseController {

    /*************       Product CRUD      *****************/

    @PostMapping(
            value = "/add-Product"
    )
    @ResponseStatus(HttpStatus.CREATED)
    Response<?> addProduct(@ModelAttribute CreateProductRequest request);

    @PutMapping("/update-Product")
    @ResponseStatus(HttpStatus.OK)
    Response<ProductDto> updateProduct(@Valid @RequestBody UpdateProductRequest request);

    @DeleteMapping("/delete-Product")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Response<?> deleteProduct(@RequestParam List<String> ids);

    @PostMapping("/search-Product")
    @ResponseStatus(HttpStatus.OK)
    Page<ProductDto> searchProduct(@Valid @RequestBody ProductSearchRequest request);

    /*************       Product Images Management      *****************/

    @PostMapping(
            value = "/add-Product-Images/{productId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    Response<?> addProductImages(
            @PathVariable String productId,
            @RequestParam("images") List<MultipartFile> images
    );

    @DeleteMapping("/delete-Product-Image/{productId}")
    @ResponseStatus(HttpStatus.OK)
    Response<?> deleteProductImage(
            @PathVariable String productId,
            @RequestParam String imageKey
    );

    @PutMapping(
            value = "/replace-Product-Images/{productId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    Response<?> replaceProductImages(
            @PathVariable String productId,
            @RequestParam("images") List<MultipartFile> images
    );

    /*************       Category CRUD      *****************/

    @PostMapping("/add-Category")
    @ResponseStatus(HttpStatus.CREATED)
    Response<String> addCategory(@Valid @RequestParam String name);

    @PutMapping("/update-Category")
    @ResponseStatus(HttpStatus.OK)
    Response<CategoryDto> updateCategory(@Valid @RequestBody CategoryDto categoryDto);

    @DeleteMapping("/delete-Category")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Response<?> deleteCategory(@RequestParam @Valid List<String> ids);

    @PostMapping("/search-Category")
    @ResponseStatus(HttpStatus.OK)
    Response<PagingResponse<CategoryDto>> searchCategory(@Valid @RequestBody CategorySearchRequest request);

    /*************       Attributes Management      *****************/

    @PostMapping("/add-Attributes")
    @ResponseStatus(HttpStatus.CREATED)
    Response<AttributesDto> addAttributes(@Valid @RequestBody CreateAttributesRequest request);

    @PutMapping("/update-Attributes")
    @ResponseStatus(HttpStatus.OK)
    Response<AttributesDto> updateAttributes(@Valid @RequestBody UpdateAttributesRequest request);

    @DeleteMapping("/delete-Attributes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Response<?> deleteAttributes(@RequestParam @Valid List<String> skus);

    @DeleteMapping("/delete-Attributes-by-Product/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Response<?> deleteAttributesByProduct(@PathVariable String productId);

    @GetMapping("/get-Attributes-by-Product/{productId}")
    @ResponseStatus(HttpStatus.OK)
    Response<List<AttributesDto>> getAttributesByProduct(@PathVariable String productId);

    @GetMapping("/get-Attributes-by-Sku/{sku}")
    @ResponseStatus(HttpStatus.OK)
    Response<AttributesDto> getAttributesBySku(@PathVariable String sku);
}
