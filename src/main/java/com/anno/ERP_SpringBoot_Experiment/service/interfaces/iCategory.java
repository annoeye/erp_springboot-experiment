package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CategorySearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.util.List;

public interface iCategory {
    Response<String> create(String name);
    Response<CategoryDto> update(final CategoryDto request);
    Response<?> delete(@NonNull final List<String> ids);
    Page<CategoryDto> search(@NonNull final CategorySearchRequest request);
}
