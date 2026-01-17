package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity;
import com.anno.ERP_SpringBoot_Experiment.model.enums.BookingStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AuditInfoDto;
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
     * Tên booking.
     */
    String name;

    /**
     * Thông tin audit (created/updated).
     */
    AuditInfoDto auditInfo;

    /**
     * Danh sách sản phẩm đặt trước với số lượng.
     */
    List<ProductQuantity> products;

    /**
     * Tên khách hàng.
     */
    String customerName;

    /**
     * Số điện thoại khách hàng.
     */
    String phoneNumber;

    /**
     * Ghi chú.
     */
    String note;

    /**
     * Địa chỉ giao hàng.
     */
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
