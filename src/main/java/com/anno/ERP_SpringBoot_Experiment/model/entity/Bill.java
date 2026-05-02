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
public class Bill extends IdentityOnly<Long> {

    /* ============================ 1.   HEADER ============================ */

    /**
     * Ngày xuất hóa đơn
     * @en Invoice date
     */
    @Column(name = "invoice_date", nullable = false)
    LocalDateTime invoiceDate;

    /* ============================ 2. BUYER ============================ */

    /**
     * Tên khách hàng (Snapshot từ Order)
     * @en Customer name
     */
    @Column(name = "customer_name", length = 200)
    String customerName;

    /**
     * SĐT khách hàng (Snapshot từ Order)
     * @en Customer phone
     */
    @Column(name = "customer_phone", length = 20)
    String customerPhone;

    /**
     * Địa chỉ (Snapshot từ Order)
     * @en Address
     */
    @Column(name = "address", length = 500)
    String address;

    /* ============================ 3. FOOTER ============================ */

    /**
     * Tổng tiền hàng
     * @en Subtotal
     */
    @Column(name = "subtotal")
    @Builder.Default
    Double subtotal = 0.0;

    /**
     * Phí ship
     * @en Shipping fee
     */
    @Column(name = "shipping_fee")
    @Builder.Default
    Double shippingFee = 0.0;

    /**
     * Tổng thanh toán (Subtotal + Ship)
     * @en Grand total
     */
    @Column(name = "grand_total")
    @Builder.Default
    Double grandTotal = 0.0;

    /**
     * Thanh toán
     * @en Payment
     */
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "payment_id")
    Payment payment;

    /**
     * Phương thức thanh toán
     * @en Payment type
     */
    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    PaymentType paymentType;

    /**
     * ID địa chỉ
     * @en Address ID
     */
    @Column(name = "id_address")
    String idAddress;


    /* ============================ RELATIONSHIPS ============================ */

    /**
     * Đơn hàng liên kết
     * @en Linked order
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    Order order;
}
