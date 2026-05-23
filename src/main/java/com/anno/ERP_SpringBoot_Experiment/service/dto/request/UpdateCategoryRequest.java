package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class UpdateCategoryRequest {
    @NotBlank
    private String id;
    private String name;
    private String description;
}
