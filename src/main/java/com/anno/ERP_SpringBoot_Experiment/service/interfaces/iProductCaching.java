package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;

import java.util.List;

public interface iProductCaching {
    void addProduct(List<ProductDto> items);
}
