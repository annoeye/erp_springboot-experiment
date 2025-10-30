package com.anno.ERP_SpringBoot_Experiment.service.dto;

import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.Specification}
 */
@Value
public class SpecificationDto implements Serializable {
    String key;
    String data;
}