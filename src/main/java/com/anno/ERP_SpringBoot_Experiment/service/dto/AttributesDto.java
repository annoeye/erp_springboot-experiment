package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for {@link Attributes}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributesDto implements Serializable {
    UUID id;
    String name;
    SkuInfoDto sku;
    double price;
    double salePrice;
    int stockQuantity;
    String color;
    String option;
    StockStatus statusProduct;
    List<SpecificationDto> specifications;
    List<PromotionDto> promotions;
    Set<String> keywords;
    AuditInfoDto auditInfo;
    ProductDto product;
}