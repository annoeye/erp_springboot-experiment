package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment extends IdentityOnly {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    Order order;

    @Column(name = "provider", length = 50)
    String provider; // VNPAY, MOMO, PAYPAL, CASH

    @Column(name = "transaction_code", length = 100)
    String transactionCode; // Mã giao dịch từ phía provider (vnp_TransactionNo)

    @Column(name = "amount")
    Double amount;

    @Column(name = "status", length = 50)
    String status; // PAID, UNPAID, FAILED

    @Column(name = "payment_date")
    LocalDateTime paymentDate;

    @Column(name = "description", length = 500)
    String description; // Nội dung thanh toán (vnp_OrderInfo)

    @Column(name = "raw_response", columnDefinition = "TEXT")
    String rawResponse; // Lưu toàn bộ response từ 3rd party để debug/audit

    @Column(name = "bank_code", length = 50)
    String bankCode; // Mã ngân hàng (NCB, VCB...)

    @Column(name = "card_type", length = 50)
    String cardType; // ATM, QRCODE, VISA...

    @Column(name = "bank_tran_no", length = 100)
    String bankTranNo; // Số giao dịch tại ngân hàng (khác với transactionCode của cổng)

    @Column(name = "ip_address", length = 50)
    String ipAddress;

    @OneToOne(mappedBy = "payment")
    Bill bill;
}
