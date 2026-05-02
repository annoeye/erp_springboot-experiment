package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.Specificationa;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for
 * {@link Specificationa}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationDto implements Serializable {
    private String key;

    @JsonAlias("value")
    private String data;
}
