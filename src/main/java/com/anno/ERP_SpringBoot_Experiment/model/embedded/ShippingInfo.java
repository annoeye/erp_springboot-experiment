package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShippingInfo {

    @Column(name = "shipping_address", length = 500)
    String address;

    @Column(name = "shipping_city", length = 100)
    String city;

    @Column(name = "shipping_district", length = 100)
    String district;

    @Column(name = "shipping_ward", length = 100)
    String ward;

    @Column(name = "shipping_postal_code", length = 20)
    String postalCode;

    @Column(name = "shipping_phone", length = 20)
    String phoneNumber;

    @Column(name = "shipping_recipient_name", length = 200)
    String recipientName;

    @Column(name = "shipping_method", length = 100)
    String shippingMethod; // Standard, Express, Same Day

    @Column(name = "shipping_carrier", length = 100)
    String carrier; // GHN, GHTK, Viettel Post, etc.

    @Column(name = "tracking_number", length = 100)
    String trackingNumber;

    @Column(name = "shipping_fee")
    Double shippingFee;

    @Column(name = "estimated_delivery_date")
    LocalDateTime estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    LocalDateTime actualDeliveryDate;

    @Column(name = "shipping_notes", length = 1000)
    String notes;
}
