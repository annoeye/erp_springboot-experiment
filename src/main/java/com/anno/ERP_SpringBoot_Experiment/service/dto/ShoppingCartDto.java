package com.anno.ERP_SpringBoot_Experiment.service.dto;

import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.entity.ShoppingCart}
 */
@Value
public class ShoppingCartDto implements Serializable {
    UUID id;
    String name;
    AuditInfoDto auditInfo;
    UserDto user;
    List<ProductQuantityDto> items;
    Integer totalItems;
    Double totalPrice;
    Double totalSalePrice;
    Double totalDiscount;
}