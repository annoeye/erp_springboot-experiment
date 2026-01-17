package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.common.annotation.NormalizedId;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ShippingInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO để cập nhật thông tin Order.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateOrderRequest {

    /**
     * ID của Order cần cập nhật (bắt buộc).
     * Được normalize tự động: uppercase + remove dashes.
     */
    @NormalizedId
    @NotNull(message = "Order ID không được để trống")
    String orderId;

    /**
     * Trạng thái mới của Order (optional).
     */
    OrderStatus status;

    /**
     * Thông tin giao hàng mới (optional).
     */
    ShippingInfo shippingInfo;

    /**
     * Ghi chú của admin (optional).
     */
    String adminNotes;

    /**
     * Mã vận đơn theo dõi (optional).
     */
    String trackingNumber;
}
