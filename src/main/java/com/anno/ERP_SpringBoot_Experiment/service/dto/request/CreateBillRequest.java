package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.common.annotation.NormalizedId;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @NotNull(message = "Mã đơn hàng không được để trống")
    String orderId;

    /**
     * Mã hóa đơn (bắt buộc).
     */
    @NotBlank(message = "Mã hóa đơn không được để trống")
    String invoiceCode;

    /**
     * Ngày xuất hóa đơn (bắt buộc).
     */
    @NotNull(message = "Ngày xuất hóa đơn không được để trống")
    LocalDateTime invoiceDate;

    /**
     * Tên khách hàng (bắt buộc).
     */
    @NotBlank(message = "Tên khách hàng không được để trống")
    String customerName;

    /**
     * Số điện thoại khách hàng (bắt buộc).
     */
    @NotBlank(message = "Số điện thoại khách hàng không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng")
    String customerPhone;

    /**
     * Email khách hàng (bắt buộc).
     */
    @NotBlank(message = "Email khách hàng không được để trống")
    @Email(message = "Email không đúng định dạng")
    String customerEmail;

    /**
     * Địa chỉ giao hàng (bắt buộc).
     */
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    String address;

    /**
     * Loại thanh toán (bắt buộc).
     */
    @NotNull(message = "Hình thức thanh toán không được để trống")
    PaymentType paymentType;

    /**
     * Phí vận chuyển.
     */
    @NotNull(message = "Phí vận chuyển không được để trống")
    Double shippingFee = 0.0;

    /**
     * Tổng tiền đơn hàng.
     */
    @NotNull(message = "Tổng tiền đơn hàng không được để trống")
    Double grandTotal = 0.0;

    /**
     * Ghi chú hóa đơn.
     */
    String note;

    /**
     * ID địa chỉ trong hệ thống (bắt buộc).
     * Được normalize tự động: uppercase + remove dashes.
     */
    @NormalizedId
    @NotNull(message = "ID địa chỉ không được để trống")
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
