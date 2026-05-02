package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.BaseEntity;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity;
import com.anno.ERP_SpringBoot_Experiment.model.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Booking")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Booking extends BaseEntity<Long> {

    /**
     * Thông tin kiểm toán
     * @en Audit info
     */
    @Embedded
    AuditInfo auditInfo = new AuditInfo();

    /**
     * Danh sách sản phẩm
     * @en Products list
     */
    @Convert(converter = com.anno.ERP_SpringBoot_Experiment.config.converter.ProductQuantityListConverter.class)
    @Column(name = "products", columnDefinition = "CLOB")
    List<ProductQuantity> products = new ArrayList<>();

    /**
     * Tên khách hàng
     * @en Customer name
     */
    String customerName;
    
    /**
     * Số điện thoại
     * @en Phone number
     */
    String phoneNumber;
    
    /**
     * Tổng giá
     * @en Total price
     */
    double totalPrice;
    
    /**
     * Ghi chú
     * @en Note
     */
    String note;
    
    /**
     * Địa chỉ
     * @en Address
     */
    String address;

    /**
     * Ngày đặt
     * @en Booking date
     */
    @Column(name = "booking_date")
    LocalDateTime bookingDate;

    /**
     * Ngày bắt đầu
     * @en Start date
     */
    @Column(name = "start_date")
    LocalDateTime startDate;

    /**
     * Trạng thái
     * @en Status
     */
    @Enumerated(EnumType.STRING)
    BookingStatus status;
}
