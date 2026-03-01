package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity;
import com.anno.ERP_SpringBoot_Experiment.model.enums.BookingStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AuditInfoDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO để tạo/cập nhật Booking (đặt hàng trước).
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingRequest {

    /**
     * Tên booking (bắt buộc).
     */
    @NotBlank(message = "Tên booking không được để trống")
    String name;

    /**
     * Thông tin audit (created/updated).
     */
    AuditInfoDto auditInfo;

    /**
     * Danh sách sản phẩm đặt trước với số lượng (bắt buộc, ít nhất 1 sản phẩm).
     */
    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    List<ProductQuantity> products;

    /**
     * Tên khách hàng (bắt buộc).
     */
    @NotBlank(message = "Tên khách hàng không được để trống")
    String customerName;

    /**
     * Số điện thoại khách hàng (bắt buộc).
     */
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng")
    String phoneNumber;

    /**
     * Ghi chú.
     */
    String note;

    /**
     * Địa chỉ giao hàng (bắt buộc).
     */
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    String address;

    /**
     * Ngày đặt booking.
     */
    LocalDateTime bookingDate;

    /**
     * Ngày bắt đầu (dự kiến giao).
     */
    LocalDateTime startDate;

    /**
     * Trạng thái booking.
     */
    BookingStatus status;
}
