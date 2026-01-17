package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.common.annotation.NormalizedId;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Request DTO để tạo Bill (hóa đơn) cho Order.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateBillRequest {

    /**
     * ID của Order cần tạo hóa đơn (bắt buộc).
     * Hỗ trợ nhiều format: id, orderId, order_id.
     * Được normalize tự động: uppercase + remove dashes.
     */
    @NormalizedId
    @JsonProperty("id")
    @JsonAlias({ "orderId", "order_id" })
    @NotNull(message = "Order ID cannot be null")
    String orderId;

    /**
     * Mã hóa đơn (bắt buộc).
     */
    @NotNull(message = "Invoice Code cannot be null")
    String invoiceCode;

    /**
     * Ngày xuất hóa đơn (bắt buộc).
     */
    @NotNull(message = "Invoice Date cannot be null")
    LocalDateTime invoiceDate;

    /**
     * Tên khách hàng (bắt buộc).
     */
    @NotNull(message = "Customer Name cannot be null")
    String customerName;

    /**
     * Số điện thoại khách hàng (bắt buộc).
     */
    @NotNull(message = "Customer Phone cannot be null")
    String customerPhone;

    /**
     * Email khách hàng (bắt buộc).
     */
    @NotNull(message = "Customer Email cannot be null")
    String customerEmail;

    /**
     * Địa chỉ giao hàng (bắt buộc).
     */
    @NotNull(message = "Address cannot be null")
    String address;

    /**
     * Loại thanh toán (bắt buộc).
     */
    @NotNull(message = "Payment Type cannot be null")
    PaymentType paymentType;

    /**
     * Phí vận chuyển.
     */
    @NotNull(message = "Shipping Fee cannot be null")
    Double shippingFee = 0.0;

    /**
     * Tổng tiền đơn hàng.
     */
    @NotNull(message = "Grand Total cannot be null")
    Double grandTotal = 0.0;

    /**
     * Ghi chú hóa đơn (bắt buộc).
     */
    @NotNull(message = "Note cannot be null")
    String note;

    /**
     * ID địa chỉ trong hệ thống (bắt buộc).
     * Được normalize tự động: uppercase + remove dashes.
     */
    @NormalizedId
    @NotNull()
    String idAddress;

    /**
     * Kỳ hạn trả góp (số tháng) - optional.
     */
    Integer installmentTerm;

    /**
     * Số tiền trả hàng tháng - optional.
     */
    Double installmentMonthlyAmount;

    /**
     * Đối tác trả góp (VD: Home Credit, FE Credit) - optional.
     */
    String installmentPartner;
}
