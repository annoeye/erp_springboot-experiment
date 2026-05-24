package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "product_inventory", indexes = {
    @Index(name = "idx_inventory_sku", columnList = "sku", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductInventory extends IdentityOnly<Long> {
    
    @Column(nullable = false, unique = true)
    String sku;
    
    @Column(nullable = false)
    Integer availableQuantity;
    
    @Column(nullable = false)
    @Builder.Default
    Integer reservedQuantity = 0;
    
    @Version
    Long version;
    
    @OneToOne
    @JoinColumn(name = "product_id", nullable = false)
    Product product;
}
