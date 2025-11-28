package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.SpecificationDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SetSpecificationsRequest {

    @JsonProperty("attributes_id")
    String attributesId;

    @JsonProperty("option")
    SpecificationOption option;

    @JsonProperty("specification_dto")
    List<SpecificationDto> specificationDto;

    StockStatus status;

    @JsonProperty("source_attributes_id")
    String sourceAttributesId;

    public enum SpecificationOption {
        CREATE_NEW,
        COPY_FROM_OTHER
    }
}
