package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.PaymentInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ShippingInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDto {
    UUID id;
    String orderNumber;
    LocalDateTime orderDate;
    OrderStatus status;

    // Customer info
    UUID customerId;
    String customerName;
    String customerEmail;
    String customerPhone;

    // Order items
    List<OrderItemDto> orderItems;

    // Pricing
    Double subtotal;
    Double discountAmount;
    String discountCode;
    Double taxAmount;
    Double shippingFee;
    Double totalAmount;

    // Shipping & Payment
    ShippingInfo shippingInfo;
    PaymentInfo paymentInfo;

    // Notes
    String customerNotes;
    String adminNotes;
    String cancellationReason;

    // Timestamps
    LocalDateTime cancelledAt;
    String cancelledBy;
    LocalDateTime confirmedAt;
    String confirmedBy;
    LocalDateTime completedAt;

    // Related entities
    String bookingId;
    String shoppingCartId;

    // Audit info
    AuditInfoDto auditInfo;
}
