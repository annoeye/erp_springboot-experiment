package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.config.Views;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.List;


/**
 * DTO for {@link Product}
 */
@Value
@Builder
public class ProductDto implements Serializable {

    @JsonView(Views.User.class)
    Long id;

    @JsonView(Views.User.class)
    String name;

    @JsonView(Views.User.class)
    SkuInfoDto skuInfo;

    @JsonView(Views.User.class)
    List<MediaItemDto> mediaItems;

    /** Trang thai san pham - Admin moi thay duoc (User chi thay san pham ACTIVE) */
    @JsonView(Views.Admin.class)
    ActiveStatus status;

    @JsonView(Views.User.class)
    Integer viewCount;

    /** Tong so luong da ban - Chi Admin/Manager thay */
    @JsonView(Views.Admin.class)
    Integer totalSoldQuantity;

    /** Tong doanh thu - Chi Admin/Manager thay */
    @JsonView(Views.Admin.class)
    java.math.BigDecimal totalRevenue;
}