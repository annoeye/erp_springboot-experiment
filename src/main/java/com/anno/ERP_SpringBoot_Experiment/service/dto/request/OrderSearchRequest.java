package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderSearchRequest {

    /**
     * Tìm theo order number
     */
    String orderNumber;

    /**
     * Tìm theo customer ID
     */
    String customerId;

    /**
     * Tìm theo customer name
     */
    String customerName;

    /**
     * Tìm theo customer email
     */
    String customerEmail;

    /**
     * Tìm theo customer phone
     */
    String customerPhone;

    /**
     * Tìm theo order status
     */
    OrderStatus orderStatus;

    /**
     * Tìm theo payment status
     */
    PaymentStatus paymentStatus;

    /**
     * Tìm theo khoảng thời gian
     */
    LocalDateTime startDate;
    LocalDateTime endDate;

    /**
     * Tìm theo khoảng giá
     */
    Double minAmount;
    Double maxAmount;

    /**
     * Pagination
     */
    Integer page;
    Integer size;

    /**
     * Sorting
     */
    String sortBy; // orderDate, totalAmount, status
    String sortDirection; // ASC, DESC
}
