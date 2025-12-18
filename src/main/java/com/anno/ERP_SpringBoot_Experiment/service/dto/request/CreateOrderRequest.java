package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.ShippingInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {

    /**
     * Danh sách items trong order
     */
    @NotEmpty(message = "Order phải có ít nhất 1 sản phẩm")
    List<OrderItemRequest> items;

    /**
     * Thông tin giao hàng
     */
    @NotNull(message = "Thông tin giao hàng không được để trống")
    ShippingInfo shippingInfo;

    /**
     * Phương thức thanh toán
     */
    @NotNull(message = "Phương thức thanh toán không được để trống")
    PaymentMethod paymentMethod;

    /**
     * Mã giảm giá (optional)
     */
    String discountCode;

    /**
     * Ghi chú của khách hàng (optional)
     */
    String customerNotes;

    /**
     * ID của booking nếu tạo từ booking (optional)
     */
    String bookingId;

    /**
     * ID của shopping cart nếu tạo từ cart (optional)
     */
    String shoppingCartId;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OrderItemRequest {
        @NotNull(message = "Attributes ID không được để trống")
        String attributesId;

        @NotNull(message = "Số lượng không được để trống")
        Integer quantity;

        String notes; // Ghi chú cho item (optional)
    }
}
