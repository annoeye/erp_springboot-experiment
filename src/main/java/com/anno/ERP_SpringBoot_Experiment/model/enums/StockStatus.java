package com.anno.ERP_SpringBoot_Experiment.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockStatus {
    AVAILABLE("Còn hàng"),
    UNAVAILABLE("Hết hàng"),
    COMING_SOON("Hàng sắp về");

    private final String value;
}
