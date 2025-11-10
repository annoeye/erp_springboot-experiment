package com.anno.ERP_SpringBoot_Experiment.service.dto;

import jakarta.persistence.Column;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductDetailsDto {
    @Column(name = "attributes_id")
    String attributesId;

    int quantity;
}
