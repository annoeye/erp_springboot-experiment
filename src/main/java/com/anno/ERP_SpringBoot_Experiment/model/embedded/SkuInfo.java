package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
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
public class SkuInfo {
    @Column(name = "sku")
    String SKU;

    public String getSku() {
        return SKU;
    }

    public void setSku(String sku) {
        this.SKU = sku;
    }
}
