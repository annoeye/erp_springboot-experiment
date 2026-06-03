package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
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

    Long id;

    String name;

    /** Thong tin SKU */
    SkuInfoDto skuInfo;

    /** Số lượng sản phẩm trong danh mục */
    Long productCount;
}
