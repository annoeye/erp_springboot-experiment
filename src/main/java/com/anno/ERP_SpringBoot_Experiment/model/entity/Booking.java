package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
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
public class Booking extends IdentityOnly {

    @Embedded
    AuditInfo auditInfo = new AuditInfo();

    @ElementCollection
    @CollectionTable(
            name = "booking_specifications",
            joinColumns = @JoinColumn(name = "booking_id")
    )
    List<ProductQuantity> products = new ArrayList<>();

    String customerName;
    String phoneNumber;
    double totalPrice;
    String note;
    String address;

    @Column(name = "booking_date")
    LocalDateTime bookingDate;

    @Column(name = "start_date")
    LocalDateTime startDate;

    @Enumerated(EnumType.STRING)
    BookingStatus status;
}
