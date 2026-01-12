package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "Bill")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Bill extends IdentityOnly {

    /* ============================ 1. HEADER ============================ */

    @Column(name = "invoice_date", nullable = false)
    LocalDateTime invoiceDate; // Ngày xuất hóa đơn

    /* ============================ 2. BUYER ============================ */

    @Column(name = "customer_name", length = 200)
    String customerName; // Tên khách hàng (Snapshot từ Order)

    @Column(name = "customer_phone", length = 20)
    String customerPhone; // SĐT khách hàng (Snapshot từ Order)

    @Column(name = "address", length = 500)
    String address; // Địa chỉ (Snapshot từ Order)

    /* ============================ 3. FOOTER ============================ */

    @Column(name = "subtotal")
    @Builder.Default
    Double subtotal = 0.0; // Tổng tiền hàng

    @Column(name = "shipping_fee")
    @Builder.Default
    Double shippingFee = 0.0; // Phí ship

    @Column(name = "grand_total")
    @Builder.Default
    Double grandTotal = 0.0; // Tổng thanh toán (Subtotal + Ship)

    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "payment_id")
    Payment payment;

    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    PaymentType paymentType;

    @Column(name = "id_address")
    String idAddress;

    /* ============================ RELATIONSHIPS ============================ */

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    Order order;
}
