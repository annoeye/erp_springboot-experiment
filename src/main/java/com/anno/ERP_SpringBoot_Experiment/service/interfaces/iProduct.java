package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateProductRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import lombok.NonNull;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.UUID;

public interface iProduct {
    Response<?> addProduct(@ModelAttribute CreateProductRequest request);
    Response<?> deleteProduct(@NonNull final List<UUID> ids);
}
