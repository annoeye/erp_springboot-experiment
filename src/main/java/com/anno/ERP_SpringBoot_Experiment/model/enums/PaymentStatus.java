package com.anno.ERP_SpringBoot_Experiment.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    PENDING("Đang xử lý"),
    PARTIAL("Thanh toán một phần"),
    PAID("Đã thanh toán"),
    REFUNDED("Đã hoàn tiền"),
    FAILED("Thanh toán thất bại"),
    CANCELLED("Đã hủy");

    private final String displayName;
}
