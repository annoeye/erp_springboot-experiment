package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shopping_cart")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShoppingCart extends IdentityOnly {
    
    @Embedded
    @Builder.Default
    AuditInfo auditInfo = new AuditInfo();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    User user;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "cart_items",
            joinColumns = @JoinColumn(name = "cart_id")
    )
    @Builder.Default
    List<ProductQuantity> items = new ArrayList<>();

    @Column(name = "total_items")
    @Builder.Default
    Integer totalItems = 0;

    @Column(name = "total_price")
    @Builder.Default
    Double totalPrice = 0.0;

    @Column(name = "total_sale_price")
    @Builder.Default
    Double totalSalePrice = 0.0;

    @Column(name = "total_discount")
    @Builder.Default
    Double totalDiscount = 0.0;

    public void addItems(List<ProductQuantity> itemsToAdd) {
        if (itemsToAdd == null || itemsToAdd.isEmpty()) {
            return;
        }

        if (this.items == null) {
            this.items = new ArrayList<>();
        }

        for (ProductQuantity itemToAdd : itemsToAdd) {
            if (itemToAdd == null) {
                continue;
            }

            boolean found = false;
            for (ProductQuantity existingItem : this.items) {
                if (existingItem.getAttributesId().equals(itemToAdd.getAttributesId())) {
                    existingItem.setQuantity(existingItem.getQuantity() + itemToAdd.getQuantity());
                    found = true;
                    break;
                }
            }

            if (!found) {
                this.items.add(itemToAdd);
            }
        }
    }

    public void removeItems(List<ProductQuantity> itemsToRemove) {
        if (itemsToRemove == null || itemsToRemove.isEmpty() || this.items == null) {
            return;
        }

        for (ProductQuantity itemToRemove : itemsToRemove) {
            this.items.removeIf(existingItem -> {
                if (existingItem.getAttributesId().equals(itemToRemove.getAttributesId())) {
                    if (itemToRemove.getQuantity() >= existingItem.getQuantity()) {
                        return true;
                    } else {
                        existingItem.setQuantity(existingItem.getQuantity() - itemToRemove.getQuantity());
                        return false;
                    }
                }
                return false;
            });
        }
    }

    public void updateTotals(Integer totalItems, Double totalPrice, Double totalSalePrice) {
        this.totalItems = totalItems != null ? totalItems : 0;
        this.totalPrice = totalPrice != null ? totalPrice : 0.0;
        this.totalSalePrice = totalSalePrice != null ? totalSalePrice : 0.0;
        this.totalDiscount = this.totalPrice - this.totalSalePrice;
    }
}
