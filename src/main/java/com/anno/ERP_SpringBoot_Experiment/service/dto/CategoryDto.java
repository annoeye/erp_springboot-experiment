package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import lombok.Value;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link Category}
 */
@Value
public class CategoryDto implements Serializable {
    UUID id;
    String name;
    SkuInfoDto skuInfo;
}