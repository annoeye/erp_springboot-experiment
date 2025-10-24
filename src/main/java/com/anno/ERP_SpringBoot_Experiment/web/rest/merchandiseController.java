package com.anno.ERP_SpringBoot_Experiment.web.rest;

import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CategorySearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequestMapping("/api/merchandise")
public interface merchandiseController {

    @PostMapping(
            value = "/add-Product",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    Response<?> addProduct(@ModelAttribute CreateProductRequest request);

    @PostMapping("/update-Category")
    @ResponseStatus(HttpStatus.OK)
    Response<CategoryDto> updateCategory(@Valid @RequestBody CategoryDto categoryDto);


    /*************       Category      *****************/
    @PostMapping("/add-Category")
    @ResponseStatus(HttpStatus.OK)
    Response<String> addCategory(@Valid String name);

    @PostMapping("/delete-Category")
    @ResponseStatus(HttpStatus.OK)
    Response<?> deleteCategory(@RequestParam @Valid final List<String> ids);

    @PostMapping("/search-Category")
    @ResponseStatus(HttpStatus.OK)
    Response<PagingResponse<CategoryDto>> searchCategory(@Valid @RequestBody CategorySearchRequest request);

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<String> upload(@RequestBody MultipartFile file) throws IOException;
}
