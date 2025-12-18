package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.PaymentInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ShippingInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_number", columnList = "order_number"),
        @Index(name = "idx_order_status", columnList = "order_status"),
        @Index(name = "idx_order_date", columnList = "order_date"),
        @Index(name = "idx_customer_id", columnList = "customer_id")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends IdentityOnly {

    /* ============================ 🔢 Order Information ============================ */

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    String orderNumber;

    @Column(name = "order_date", nullable = false)
    LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    OrderStatus status;

    /* ============================ 👤 Customer Information ============================ */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_order_customer")
    )
    @ToString.Exclude
    User customer;

    @Column(name = "customer_name", nullable = false, length = 200)
    String customerName; // Lưu tên khách hàng tại thời điểm đặt hàng

    @Column(name = "customer_email", length = 200)
    String customerEmail;

    @Column(name = "customer_phone", length = 20)
    String customerPhone;

    /* ============================ 📦 Order Items ============================ */

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    List<OrderItem> orderItems = new ArrayList<>();

    /* ============================ 💰 Pricing Information ============================ */

    @Column(name = "subtotal", nullable = false)
    @Builder.Default
    Double subtotal = 0.0; // Tổng tiền hàng (chưa bao gồm phí ship, thuế)

    @Column(name = "discount_amount")
    @Builder.Default
    Double discountAmount = 0.0; // Tổng giảm giá

    @Column(name = "discount_code", length = 100)
    String discountCode; // Mã giảm giá đã áp dụng

    @Column(name = "tax_amount")
    @Builder.Default
    Double taxAmount = 0.0; // Thuế

    @Column(name = "shipping_fee")
    @Builder.Default
    Double shippingFee = 0.0; // Phí vận chuyển

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    Double totalAmount = 0.0; // Tổng tiền phải trả

    /* ============================ 🚚 Shipping Information ============================ */

    /* ======================= 🚚 Shipping Information ======================= */

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "shippingFee", column = @Column(name = "info_shipping_fee"))
    })
    @Builder.Default
    ShippingInfo shippingInfo = new ShippingInfo();

    /* ============================ 💳 Payment Information ============================ */

    @Embedded
    @Builder.Default
    PaymentInfo paymentInfo = new PaymentInfo();

    /* ============================ 📝 Additional Information ============================ */

    @Column(name = "customer_notes", length = 2000)
    String customerNotes; // Ghi chú của khách hàng

    @Column(name = "admin_notes", length = 2000)
    String adminNotes; // Ghi chú nội bộ

    @Column(name = "cancellation_reason", length = 1000)
    String cancellationReason; // Lý do hủy đơn

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    String cancelledBy; // User ID người hủy

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    @Column(name = "confirmed_by")
    String confirmedBy; // User ID người xác nhận

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    /* ============================ 🔗 Related Entities ============================ */

    @Column(name = "booking_id")
    String bookingId; // Liên kết với Booking nếu order được tạo từ booking

    @Column(name = "shopping_cart_id")
    String shoppingCartId; // Liên kết với ShoppingCart nếu order được tạo từ cart

    /* ============================ 🧩 Embedded Fields ============================ */

    @Embedded
    @Builder.Default
    AuditInfo auditInfo = new AuditInfo();

    /* ============================ 🔧 Helper Methods ============================ */

    /**
     * Thêm order item vào đơn hàng
     */
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    /**
     * Xóa order item khỏi đơn hàng
     */
    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }

    /**
     * Tính toán lại tổng tiền đơn hàng
     */
    public void calculateTotals() {
        // Tính subtotal từ các order items
        this.subtotal = orderItems.stream()
                .mapToDouble(item -> item.getSubtotal() != null ? item.getSubtotal() : 0.0)
                .sum();

        // Tính tổng thuế
        this.taxAmount = orderItems.stream()
                .mapToDouble(item -> item.getTaxAmount() != null ? item.getTaxAmount() : 0.0)
                .sum();

        // Tính tổng tiền = subtotal - discount + shipping + tax
        this.totalAmount = this.subtotal - this.discountAmount + this.shippingFee + this.taxAmount;
        this.totalAmount = Math.max(0, this.totalAmount); // Đảm bảo không âm
    }

    /**
     * Kiểm tra xem đơn hàng có thể hủy không
     */
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING ||
               status == OrderStatus.CONFIRMED ||
               status == OrderStatus.PROCESSING;
    }

    /**
     * Kiểm tra xem đơn hàng có thể hoàn trả không
     */
    public boolean canBeReturned() {
        return status == OrderStatus.DELIVERED ||
               status == OrderStatus.COMPLETED;
    }

    /**
     * Kiểm tra xem đơn hàng đã được thanh toán chưa
     */
    public boolean isPaid() {
        return paymentInfo != null &&
               paymentInfo.getPaymentStatus() == com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentStatus.PAID;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Order order = (Order) o;
        return getId() != null && Objects.equals(getId(), order.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
