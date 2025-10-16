package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.request.updateCategoryRequest;

public interface iCategory {
    void create(String name);
    void update(updateCategoryRequest request);
    void delete(String sku);
}
