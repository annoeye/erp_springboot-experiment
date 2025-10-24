package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.MediaItem;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import jakarta.persistence.Column;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDto {
    String name;

    @Column(nullable = false)
    String sku;

    List<MediaItem> mediaItems;

    String skuCategory;

    ActiveStatus status;
}
