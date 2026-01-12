package com.anno.ERP_SpringBoot_Experiment.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentType {

    PAYMENT_UPON_DELIVERY("Thanh toán khi nhận hàng"),
    NCB("Thanh toán bằng NCB thông qua VNPay"),
    MOMO("Thanh toán qua Momo"),
    BUY_NOW_PAY_LATER("Mua trước trả sau");
    private final String description;
}
