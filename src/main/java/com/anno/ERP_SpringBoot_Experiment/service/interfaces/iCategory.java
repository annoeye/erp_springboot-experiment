package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CategorySearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateCategoryRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.CategoryCreateResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.CategoryExitingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.util.List;

public interface iCategory {
    Response<CategoryCreateResponse> create(String name);
    Response<?> update(final UpdateCategoryRequest request);
    Response<?> delete(@NonNull final List<String> ids);
    Page<CategoryDto> search(@NonNull final CategorySearchRequest request);
    CategoryExitingResponse isExiting(String name);
}
