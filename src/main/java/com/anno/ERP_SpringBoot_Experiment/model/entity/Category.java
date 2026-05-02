package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.BaseEntity;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Category", indexes = {
        @Index(name = "idx_category_id", columnList = "id",  unique = true),
})
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category extends BaseEntity<Long> {

    /**
     * Thông tin kiểm toán
     * @en Audit info
     */
    @Embedded
    @Builder.Default
    AuditInfo auditInfo = new AuditInfo();

    /**
     * Thông tin SKU
     * @en SKU info
     */
    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "sku", column = @Column(name = "sku_name")))
    SkuInfo skuInfo;

    /**
     * Danh sách sản phẩm
     * @en Products list
     */
    @OneToMany(
            mappedBy = "category",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    @Builder.Default
    List<Product> products = new ArrayList<>();
}
