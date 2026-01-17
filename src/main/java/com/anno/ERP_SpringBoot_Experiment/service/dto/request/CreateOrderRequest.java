package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.common.annotation.NormalizedId;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ShippingInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request DTO để tạo Order mới.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {

    /**
     * Danh sách sản phẩm trong đơn hàng (bắt buộc, ít nhất 1 item).
     */
    @NotEmpty(message = "Order phải có ít nhất 1 sản phẩm")
    List<OrderItemRequest> items;

    /**
     * Thông tin giao hàng (bắt buộc).
     */
    @NotNull(message = "Thông tin giao hàng không được để trống")
    ShippingInfo shippingInfo;

    /**
     * Phương thức thanh toán (bắt buộc).
     */
    @NotNull(message = "Phương thức thanh toán không được để trống")
    PaymentMethod paymentMethod;

    /**
     * Mã giảm giá (optional).
     */
    String discountCode;

    /**
     * Ghi chú của khách hàng (optional).
     */
    String customerNotes;

    /**
     * ID của Booking nếu tạo từ booking (optional).
     * Được normalize tự động: uppercase + remove dashes.
     */
    @NormalizedId
    String bookingId;

    /**
     * ID của Shopping Cart nếu tạo từ giỏ hàng (optional).
     * Được normalize tự động: uppercase + remove dashes.
     */
    @NormalizedId
    String shoppingCartId;

    /**
     * Chi tiết một sản phẩm trong Order.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OrderItemRequest {

        /**
         * ID của Attributes (variant sản phẩm) - bắt buộc.
         * Được normalize tự động: uppercase + remove dashes.
         */
        @NormalizedId
        @NotNull(message = "Attributes ID không được để trống")
        String attributesId;

        /**
         * Số lượng đặt mua (bắt buộc).
         */
        @NotNull(message = "Số lượng không được để trống")
        Integer quantity;

        /**
         * Ghi chú cho item (optional).
         */
        String notes;
    }
}
