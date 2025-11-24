package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.base.SkuAware;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.MediaItem;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.listener.SkuEntityListener;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

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
@EntityListeners(SkuEntityListener.class)
public class Product extends IdentityOnly implements SkuAware {

    @Embedded
    AuditInfo auditInfo;

    @Embedded
    @Column(nullable=false)
    @Builder.Default
    SkuInfo skuInfo = new SkuInfo();

    @ElementCollection
    @CollectionTable(name = "media_items",
            joinColumns = @JoinColumn(name = "product_uuid"))
    @Builder.Default
    List<MediaItem> mediaItems = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "category_uuid",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_product_category")
    )
    @ToString.Exclude
    Category category;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
        @Builder.Default
    List<Attributes> attributes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    ActiveStatus status;


    String description;
}

