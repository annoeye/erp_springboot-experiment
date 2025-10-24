package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.base.SkuAware;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.listener.SkuEntityListener;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "Category")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(SkuEntityListener.class)
public class Category extends IdentityOnly implements SkuAware {

    @Embedded
    AuditInfo auditInfo = new AuditInfo();

    @Embedded
    SkuInfo skuInfo = new SkuInfo();
}
