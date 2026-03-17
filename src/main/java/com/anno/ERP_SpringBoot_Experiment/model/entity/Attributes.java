package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.Promotion;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SpecificationGroup;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.VariantOption;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Attributes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Attributes extends IdentityOnly {
        @Embedded
        @AttributeOverrides({
                        @AttributeOverride(name = "sku", column = @Column(name = "sku_name"))
        })
        @JsonIgnore
        SkuInfo sku = new SkuInfo();

        @Column(name = "price")
        double price;

        @Column(name = "sale_price")
        double salePrice;

        @Column(name = "stock_quantity")
        int stockQuantity;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "variant_options", columnDefinition = "CLOB")
        List<VariantOption> variantOptions = new ArrayList<>();

        @Column(name = "status_product")
        @Enumerated(EnumType.STRING)
        StockStatus statusProduct;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "specifications", columnDefinition = "CLOB")
        List<SpecificationGroup> specifications = new ArrayList<>();

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "keywords", columnDefinition = "CLOB")
        Set<String> keywords = new HashSet<>();

        @Embedded
        AuditInfo auditInfo = new AuditInfo();

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "promotions", columnDefinition = "CLOB")
        List<Promotion> promotions = new ArrayList<>();

        /*
         * ============================ 📊 Analytics Fields ============================
         */

        @Column(name = "cost_price")
        Double costPrice = 0.0;

        @Column(name = "sold_quantity")
        Integer soldQuantity = 0;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "product_id")
        @OnDelete(action = OnDeleteAction.CASCADE)
        Product product;
}
