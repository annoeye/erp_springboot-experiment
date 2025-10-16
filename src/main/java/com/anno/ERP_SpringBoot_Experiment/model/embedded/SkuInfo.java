package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import com.anno.ERP_SpringBoot_Experiment.model.listener.SkuEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EntityListeners;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(SkuEntityListener.class)
public class SkuInfo {
    @Column(name = "sku")
    String SKU;
}
