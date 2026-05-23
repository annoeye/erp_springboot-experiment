package com.anno.ERP_SpringBoot_Experiment.service.dto;

import lombok.Data;
import java.util.List;

@Data
public class VariantOptionDto {
    private String name;
    private List<String> values;
}
