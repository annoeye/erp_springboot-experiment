package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.model.listener.AuditEntityListener;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "Attributes")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditEntityListener.class)
public class Attributes extends IdentityOnly {
    @Embedded
    SkuInfo sku;

    @Column(name = "price")
    double price;

    @Column(name = "sale_price")
    double salePrice;

    @Column(name = "stock_quantity")
    int stockQuantity;

    @Column(name = "status_product")
    @Enumerated(EnumType.STRING)
    StockStatus statusProduct;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "specifications",
            columnDefinition = "json"
    )
    Map<String, String> specifications = new HashMap<>();

    @Column(name = "key_words")
    Set<String> keywords;

    @Enumerated(EnumType.STRING)
    ActiveStatus status;
}
