package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAttributesRequest {
    String name;
    double price;
    double salePrice;
    int stockQuantity;
    StockStatus statusProduct;
    List<String> data;
    Set<String> keywords;
    String productId;
}
