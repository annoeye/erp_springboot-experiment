package com.anno.ERP_SpringBoot_Experiment.service.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetProductRequest {
    String name;
    String description;


}
