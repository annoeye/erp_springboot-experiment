package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("address_id")
    @NotNull(message = "Thông tin giao hàng không được để trống")
    String addressId;

    /**
     * Mã giảm giá (optional).
     */
    @JsonProperty("discount_code")
    String discountCode;

    /**
     * Ghi chú của khách hàng (optional).
     */
    @JsonProperty("customer_notes")
    String customerNotes;

    /**
     * Thông tin thanh toán.
     */
    @JsonProperty("shipping_method")
    String shippingMethod;

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
        @NotNull(message = "Attributes sku không được để trống")
        String attributesSku;

        /**
         * Số lượng đặt mua (bắt buộc).
         */
        @NotNull(message = "Số lượng không được để trống")
        Integer quantity;
    }
}
