package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.MediaItem;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Product")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product extends IdentityOnly {

        @Embedded
        @Builder.Default
        AuditInfo auditInfo = new AuditInfo();

        @Embedded
        @Column(nullable = false)
        @Builder.Default
        SkuInfo skuInfo = new SkuInfo();

        @Convert(converter = com.anno.ERP_SpringBoot_Experiment.config.converter.MediaItemListConverter.class)
        @Column(name = "media_items", columnDefinition = "CLOB")
        @Builder.Default
        List<MediaItem> mediaItems = new ArrayList<>();

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "category_uuid", nullable = false, foreignKey = @ForeignKey(name = "FK_product_category"))
        @ToString.Exclude
        @OnDelete(action = OnDeleteAction.CASCADE)
        @JsonIgnore
        Category category;

        @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        @Builder.Default
        @JsonIgnore
        List<Attributes> attributes = new ArrayList<>();

        @Enumerated(EnumType.STRING)
        ActiveStatus status;

        /*
         * ============================ 📊 Analytics Fields ============================
         */

        @Column(name = "total_sold_quantity")
        @Builder.Default
        Integer totalSoldQuantity = 0; // Tổng số lượng đã bán

        @Column(name = "total_revenue")
        @Builder.Default
        Double totalRevenue = 0.0; // Tổng doanh thu

        @Column(name = "total_orders")
        @Builder.Default
        Integer totalOrders = 0; // Số đơn hàng chứa sản phẩm này

        @Column(name = "view_count")
        @Builder.Default
        Integer viewCount = 0; // Số lượt xem (cho conversion rate)

        @Column(name = "average_rating")
        @Builder.Default
        Double averageRating = 0.0; // Đánh giá trung bình

        @Column(name = "review_count")
        @Builder.Default
        Integer reviewCount = 0; // Số lượng đánh giá
}
