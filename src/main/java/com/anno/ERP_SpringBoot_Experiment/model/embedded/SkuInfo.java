package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkuInfo {
    String sku;

    public SkuInfo createSku(@NonNull String name) {
        return new SkuInfo(this.sku = name.replaceAll("\\d", "").toUpperCase());
    }
}
