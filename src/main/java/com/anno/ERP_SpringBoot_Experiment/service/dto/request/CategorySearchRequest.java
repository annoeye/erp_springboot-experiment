package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategorySearchRequest {
    List<String> ids;
    List<String> skus;
    List<String> names;

    PagingRequest paging = new PagingRequest();
}
