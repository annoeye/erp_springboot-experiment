package com.anno.ERP_SpringBoot_Experiment.service.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemDto {
    UUID id;
    UUID orderId;
    UUID productId;
    UUID attributesId;
    String productName;
    String productSku;
    String attributesSku;
    String color;
    String option;
    Integer quantity;
    Double unitPrice;
    Double salePrice;
    Double discountAmount;
    Double discountPercentage;
    Double subtotal;
    Double taxAmount;
    String notes;
    String imageUrl;
}
