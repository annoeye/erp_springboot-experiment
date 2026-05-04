package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Address;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDto {
    Long id;
    String orderNumber;
    List<OrderStatus> status;
    OrderStatus currentStatus;

    // Customer info
    Long customerId;
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

    // Shipping
    Address shippingInfo;

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
