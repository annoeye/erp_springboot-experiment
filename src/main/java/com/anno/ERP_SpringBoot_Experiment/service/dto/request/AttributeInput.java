package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.VariantOption;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.Promotion;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SpecificationGroup;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;

@Data
public class AttributeInput {
    private String name;
    private String value;
    private BigDecimal price;
    private BigDecimal salePrice;
    private List<VariantOption> variantOptions;
    private List<Promotion> promotions;
    private List<SpecificationGroup> specifications;
    private StockStatus statusProduct;
}
