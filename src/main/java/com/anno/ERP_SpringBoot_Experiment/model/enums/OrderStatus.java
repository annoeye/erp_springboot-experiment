package com.anno.ERP_SpringBoot_Experiment.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PROCESSING("Đang xử lý"),
    SHIPPED("Đang giao hàng"),
    DELIVERED("Đã giao hàng"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy"),
    RETURNING("Hoàn trả hàng"),
    RETURNED("Đã trả hàng"),
    REFUNDED("Đã hoàn tiền");

    private final String displayName;
}
