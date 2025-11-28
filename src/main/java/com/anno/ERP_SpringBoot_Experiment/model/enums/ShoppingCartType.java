package com.anno.ERP_SpringBoot_Experiment.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShoppingCartType {
    SHOPPING_CART("Trạng thái của Giỏ hàng.");

    private final String description;

}
