package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.config.Views;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;


/**
 * DTO for {@link Attributes}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributesDto implements Serializable {

    @JsonView(Views.User.class)
    Long id;

    @JsonView(Views.User.class)
    String name;

    @JsonView(Views.User.class)
    SkuInfoDto sku;

    @JsonView(Views.User.class)
    double price;

    @JsonView(Views.User.class)
    double salePrice;

    /**
     * So luong ton kho chinh xac - Chi Admin thay.
     * User se thay thong qua truong statusProduct (IN_STOCK / OUT_OF_STOCK).
     */
    @JsonView(Views.Admin.class)
    int stockQuantity;

    @JsonView(Views.User.class)
    List<VariantOptionDto> variantOptions;

    /** Trang thai ton kho (IN_STOCK, OUT_OF_STOCK, LOW_STOCK) - User thay duoc */
    @JsonView(Views.User.class)
    StockStatus statusProduct;

    @JsonView(Views.User.class)
    List<SpecificationGroupDto> specifications;

    @JsonView(Views.User.class)
    List<PromotionDto> promotions;

    @JsonView(Views.User.class)
    Set<String> keywords;

    /** Thong tin kiem toan - Chi Admin thay */
    @JsonView(Views.Admin.class)
    AuditInfoDto auditInfo;

    /** Thong tin san pham cha - Chi Admin thay (User da biet san pham tu context) */
    @JsonView(Views.Admin.class)
    ProductDto product;
}