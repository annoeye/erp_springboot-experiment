package com.anno.ERP_SpringBoot_Experiment.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    VNBANK("Chuyển khoản ngân hàng VNPay"),
    CARD("Khách có thẻ Visa/Master"),
    PAYPAL("Khách có tài khoản PayPal"),
    GOOGLE_PAY("GOOGLE_PAY"),
    APPLE_PAY("APPLE_PAY"),
    COD("Thanh toán khi nhận hàng");

    private final String displayName;
}
