package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.config.Views;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Address;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonView;
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

    @JsonView(Views.User.class)
    Long id;

    @JsonView(Views.User.class)
    String orderNumber;

    @JsonView(Views.User.class)
    List<OrderStatus> status;

    @JsonView(Views.User.class)
    OrderStatus currentStatus;

    // Customer info
    /** Customer ID noi bo - Chi Admin thay */
    @JsonView(Views.Admin.class)
    Long customerId;

    @JsonView(Views.User.class)
    String customerName;

    @JsonView(Views.User.class)
    String customerEmail;

    @JsonView(Views.User.class)
    String customerPhone;

    // Order items
    @JsonView(Views.User.class)
    List<OrderItemDto> orderItems;

    // Pricing
    @JsonView(Views.User.class)
    Double subtotal;

    @JsonView(Views.User.class)
    Double discountAmount;

    @JsonView(Views.User.class)
    String discountCode;

    @JsonView(Views.User.class)
    Double taxAmount;

    @JsonView(Views.User.class)
    Double shippingFee;

    @JsonView(Views.User.class)
    Double totalAmount;

    // Shipping
    @JsonView(Views.User.class)
    Address shippingInfo;

    // Notes
    @JsonView(Views.User.class)
    String customerNotes;

    /** Ghi chu noi bo cua admin - Chi Admin thay */
    @JsonView(Views.Admin.class)
    String adminNotes;

    @JsonView(Views.User.class)
    String cancellationReason;

    // Timestamps
    @JsonView(Views.User.class)
    LocalDateTime cancelledAt;

    /** Nguoi huy don - Chi Admin thay */
    @JsonView(Views.Admin.class)
    String cancelledBy;

    @JsonView(Views.User.class)
    LocalDateTime confirmedAt;

    /** Nguoi xac nhan don - Chi Admin thay */
    @JsonView(Views.Admin.class)
    String confirmedBy;

    @JsonView(Views.User.class)
    LocalDateTime completedAt;

    // Related entities
    /** Booking ID noi bo - Chi Admin thay */
    @JsonView(Views.Admin.class)
    String bookingId;

    /** Shopping Cart ID noi bo - Chi Admin thay */
    @JsonView(Views.Admin.class)
    String shoppingCartId;

    /** Lich su kiem toan - Chi Admin thay */
    @JsonView(Views.Admin.class)
    AuditInfoDto auditInfo;
}
