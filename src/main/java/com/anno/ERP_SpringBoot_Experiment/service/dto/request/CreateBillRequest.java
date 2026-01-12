package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateBillRequest {

    @NotNull(message = "Order ID cannot be null")
    String orderId;

    @NotNull(message = "Invoice Code cannot be null")
    String invoiceCode;

    @NotNull(message = "Invoice Date cannot be null")
    LocalDateTime invoiceDate;

    @NotNull(message = "Customer Name cannot be null")
    String customerName;

    @NotNull(message = "Customer Phone cannot be null")
    String customerPhone;

    @NotNull(message = "Customer Email cannot be null")
    String customerEmail;

    @NotNull(message = "Address cannot be null")
    String address;

    @NotNull(message = "Payment Type cannot be null")
    PaymentType paymentType;

    @NotNull(message = "Shipping Fee cannot be null")
    Double shippingFee = 0.0;

    @NotNull(message = "Grand Total cannot be null")
    Double grandTotal = 0.0;

    @NotNull(message = "Note cannot be null")
    String note;

    String OrderId;

    @NotNull()
    String idAddress;

    Integer installmentTerm;
    Double installmentMonthlyAmount;
    String installmentPartner;
}
