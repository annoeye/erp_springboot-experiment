package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for
 * {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.Specification}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationDto implements Serializable {
    private String key;

    @JsonAlias("value")
    private String data;
}
