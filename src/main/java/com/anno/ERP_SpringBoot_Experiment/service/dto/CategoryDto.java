package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.config.Views;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * DTO for {@link Category}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto implements Serializable {

    @JsonView(Views.User.class)
    Long id;

    @JsonView(Views.User.class)
    String name;

    /** Thong tin SKU noi bo - Chi Admin thay */
    @JsonView(Views.Admin.class)
    SkuInfoDto skuInfo;
}