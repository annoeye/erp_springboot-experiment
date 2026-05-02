package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.BaseEntity;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Attributes", indexes = {
                @Index(name = "idx_attributes_product", columnList = "product_id"),
                @Index(name = "idx_attributes_status", columnList = "status_product"),
                @Index(name = "idx_attributes_sku", columnList = "sku_name")
})
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Attributes extends BaseEntity      <Long> {
        /**
         * Thông tin SKU (Mã sản phẩm)
         * 
         * @en SKU info
         */
        @Embedded
        @AttributeOverrides({
                        @AttributeOverride(name = "sku", column = @Column(name = "sku_name"))
        })
        @JsonIgnore
        SkuInfo sku;

        /**
         * Giá
         * 
         * @en Price
         */
        @Column(name = "price")
        double price;

        /**
         * Giá khuyến mãi
         * 
         * @en Sale price
         */
        @Column(name = "sale_price")
        double salePrice;

        /**
         * Số lượng tồn kho
         * 
         * @en Stock quantity
         */
        @Column(name = "stock_quantity")
        int stockQuantity;

        /**
         * Các tùy chọn biến thể
         * 
         * @en Variant options
         */
        @Convert(converter = com.anno.ERP_SpringBoot_Experiment.config.converter.VariantOptionListConverter.class)
        @Column(name = "variant_options", columnDefinition = "CLOB")
        @Builder.Default
        List<VariantOption> variantOptions = new ArrayList<>();

        /**
         * Trạng thái sản phẩm
         * 
         * @en Product status
         */
        @Column(name = "status_product")
        @Enumerated(EnumType.STRING)
        StockStatus statusProduct;

        /**
         * Thông số kỹ thuật
         * 
         * @en Specifications
         */
        @Convert(converter = com.anno.ERP_SpringBoot_Experiment.config.converter.SpecificationGroupListConverter.class)
        @Column(name = "specifications", columnDefinition = "CLOB")
        @Builder.Default
        List<SpecificationGroup> specifications = new ArrayList<>();

        /**
         * Từ khóa
         * 
         * @en Keywords
         */
        @Convert(converter = com.anno.ERP_SpringBoot_Experiment.config.converter.StringSetConverter.class)
        @Column(name = "keywords", columnDefinition = "CLOB")
        @Builder.Default
        Set<String> keywords = new HashSet<>();

        /**
         * Thông tin kiểm toán
         * 
         * @en Audit info
         */
        @Embedded
        @Builder.Default
        AuditInfo auditInfo = new AuditInfo();

        /**
         * Các chương trình khuyến mãi
         * 
         * @en Promotions
         */
        @Convert(converter = com.anno.ERP_SpringBoot_Experiment.config.converter.PromotionListConverter.class)
        @Column(name = "promotions", columnDefinition = "CLOB")
        @Builder.Default
        List<Promotion> promotions = new ArrayList<>();

        /*
         * ============================ 📊 Analytics Fields ============================
         */

        /**
         * Giá vốn
         * 
         * @en Cost price
         */
        @Column(name = "cost_price")
        @Builder.Default
        Double costPrice = 0.0;

        /**
         * Số lượng đã bán
         * 
         * @en Sold quantity
         */
        @Column(name = "sold_quantity")
        @Builder.Default
        Integer soldQuantity = 0;

        /**
         * Tổng số đơn hàng
         * 
         * @en Total orders
         */
        @Column(name = "total_orders")
        @Builder.Default
        Integer totalOrders = 0;

        /**
         * Sản phẩm liên kết
         * 
         * @en Linked product
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "product_id")
        @OnDelete(action = OnDeleteAction.CASCADE)
        Product product;
}
