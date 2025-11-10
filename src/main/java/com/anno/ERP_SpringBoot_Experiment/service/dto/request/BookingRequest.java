package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity;
import com.anno.ERP_SpringBoot_Experiment.model.enums.BookingStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AuditInfoDto;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingRequest {

    String name;
    AuditInfoDto auditInfo;
    List<ProductQuantity> products;
    String customerName;
    String phoneNumber;
    String note;
    String address;
    LocalDateTime bookingDate;
    LocalDateTime startDate;
    BookingStatus status;




}
