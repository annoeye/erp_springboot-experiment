package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Booking;
import com.anno.ERP_SpringBoot_Experiment.model.enums.BookingStatus;
import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;


/**
 * DTO for {@link Booking}
 */
@Value
public class BookingDto implements Serializable {
    Long id;
    String name;
    AuditInfoDto auditInfo;
    List<ProductQuantityDto> products;
    String customerName;
    String phoneNumber;
    double totalPrice;
    String note;
    String address;
    LocalDateTime bookingDate;
    LocalDateTime startDate;
    BookingStatus status;
}