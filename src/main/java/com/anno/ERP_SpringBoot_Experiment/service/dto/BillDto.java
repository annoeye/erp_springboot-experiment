package com.anno.ERP_SpringBoot_Experiment.service.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * DTO for {@link Bill}
 */
@Value
public class BillDto implements Serializable {
    Long id;
    String name;
    LocalDateTime invoiceDate;
    String customerName;
    String customerPhone;
    String address;
    Double subtotal;
    Double shippingFee;
    Double grandTotal;
    String idAddress;
    OrderDto order;
}