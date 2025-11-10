package com.anno.ERP_SpringBoot_Experiment.service.dto;

import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity}
 */
@Value
public class ProductQuantityDto implements Serializable {
    String AttributesId;
    int quantity;
}