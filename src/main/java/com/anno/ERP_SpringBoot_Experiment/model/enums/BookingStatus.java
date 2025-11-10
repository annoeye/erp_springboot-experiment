package com.anno.ERP_SpringBoot_Experiment.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    CANCELLED("Đã hủy"),
    COMPLETED("Hoàn thành"),
    EXPIRED("Hết hạn");

    private final String displayName;
}
