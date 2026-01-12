package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Product}
 */
@Value
@Builder
public class ProductDto implements Serializable {
    UUID id;
    String name;
    SkuInfoDto skuInfo;
    List<MediaItemDto> mediaItems;
    ActiveStatus status;
}