package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem extends IdentityOnly {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_order_item_order")
    )
    @ToString.Exclude
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_order_item_product")
    )
    @ToString.Exclude
    Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "attributes_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_order_item_attributes")
    )
    @ToString.Exclude
    Attributes attributes;

    @Column(name = "product_name", nullable = false, length = 500)
    String productName; // Lưu tên sản phẩm tại thời điểm đặt hàng

    @Column(name = "product_sku", length = 100)
    String productSku; // SKU của sản phẩm

    @Column(name = "attributes_sku", length = 100)
    String attributesSku; // SKU của attributes

    @Column(name = "color", length = 100)
    String color;

    @Column(name = "option_name", length = 100)
    String option; // Size, version, etc.

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "unit_price", nullable = false)
    Double unitPrice; // Giá gốc tại thời điểm đặt hàng

    @Column(name = "sale_price", nullable = false)
    Double salePrice; // Giá bán tại thời điểm đặt hàng

    @Column(name = "discount_amount")
    @Builder.Default
    Double discountAmount = 0.0; // Số tiền giảm giá cho item này

    @Column(name = "discount_percentage")
    @Builder.Default
    Double discountPercentage = 0.0; // % giảm giá

    @Column(name = "subtotal", nullable = false)
    Double subtotal; // Tổng tiền cho item này (salePrice * quantity - discountAmount)

    @Column(name = "tax_amount")
    @Builder.Default
    Double taxAmount = 0.0; // Thuế cho item này

    @Column(name = "notes", length = 1000)
    String notes; // Ghi chú cho item

    @Column(name = "image_url", length = 500)
    String imageUrl; // Ảnh sản phẩm tại thời điểm đặt hàng

    /**
     * Tính toán subtotal dựa trên quantity, salePrice và discount
     */
    public void calculateSubtotal() {
        if (quantity != null && salePrice != null) {
            double total = quantity * salePrice;
            if (discountAmount != null) {
                total -= discountAmount;
            }
            this.subtotal = Math.max(0, total);
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        OrderItem orderItem = (OrderItem) o;
        return getId() != null && Objects.equals(getId(), orderItem.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
