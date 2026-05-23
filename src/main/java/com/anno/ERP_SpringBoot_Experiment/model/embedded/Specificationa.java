package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Specificationa {
    private String name;
    private String value;
}
