package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.listener.AuditEntityListener;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Product")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditEntityListener.class)
public class Product extends IdentityOnly {

    @Embedded
    AuditInfo auditInfo;

    @Embedded
    @Column(nullable= false)
    SkuInfo sku;

    @Column(name = "media_items")
    List<String> mediaItems; // Danh sách ảnh và video

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "category_sku",
            columnDefinition = "sku",
            nullable = false
    )
    Category category;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<Attributes> attributes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    ActiveStatus status;
}

