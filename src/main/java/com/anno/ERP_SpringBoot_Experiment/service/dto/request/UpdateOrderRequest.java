package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.ShippingInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateOrderRequest {

    @NotNull(message = "Order ID không được để trống")
    String orderId;

    /**
     * Cập nhật status (optional)
     */
    OrderStatus status;

    /**
     * Cập nhật thông tin giao hàng (optional)
     */
    ShippingInfo shippingInfo;

    /**
     * Ghi chú của admin (optional)
     */
    String adminNotes;

    /**
     * Tracking number (optional)
     */
    String trackingNumber;
}
