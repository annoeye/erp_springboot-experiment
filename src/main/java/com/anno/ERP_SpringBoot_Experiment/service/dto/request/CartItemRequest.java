package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import lombok.Value;

@Value
public class CartItemRequest {
    String sku;
    int quantity;
}
