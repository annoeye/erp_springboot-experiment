package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import lombok.NonNull;

import java.util.List;
import org.springframework.data.domain.Page;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributesSearchRequest;

public interface iAttributes {

    // Smart create: returns List (works for both single and batch)
    Response<List<AttributesDto>> create(@NonNull CreateAttributesRequest request);

    Response<?> update(@NonNull UpdateAttributesRequest request);

    Response<?> delete(@NonNull List<String> skus);

    Response<?> deleteByProduct(@NonNull String productId);

    Page<AttributesDto> search(@NonNull AttributesSearchRequest request);
}
