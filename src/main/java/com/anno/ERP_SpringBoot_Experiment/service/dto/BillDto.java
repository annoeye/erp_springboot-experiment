package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Bill;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentType;
import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link Bill}
 */
@Value
public class BillDto implements Serializable {
    UUID id;
    String name;
    LocalDateTime invoiceDate;
    String customerName;
    String customerPhone;
    String address;
    Double subtotal;
    Double shippingFee;
    Double grandTotal;
    PaymentDto payment;
    PaymentType paymentType;
    String idAddress;
    OrderDto order;
}