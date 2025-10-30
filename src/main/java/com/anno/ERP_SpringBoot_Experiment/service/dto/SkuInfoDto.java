package com.anno.ERP_SpringBoot_Experiment.service.dto;

import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo}
 */
@Value
public class SkuInfoDto implements Serializable {
    String SKU;
}