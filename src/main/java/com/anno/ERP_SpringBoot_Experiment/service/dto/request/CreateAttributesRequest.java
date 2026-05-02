package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PromotionDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.SpecificationGroupDto;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;

/**
 * Request DTO để tạo Attributes (variant) cho Product.
 * Tự động tạo tổ hợp từ danh sách variantGroups.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAttributesRequest {

    /**
     * Tên variant (bắt buộc).
     */
    @NotBlank(message = "Không được để trống name.")
    String name;

    /**
     * Giá bán (bắt buộc).
     */
    @NotNull(message = "Không được để trống giá.")
    double price;

    /**
     * Giá khuyến mãi (optional).
     */
    double salePrice;

    /**
     * Số lượng tồn kho (bắt buộc).
     */
    @NotNull(message = "Không được để trống số lượng")
    int stockQuantity;

    /**
     * Trạng thái tồn kho (bắt buộc).
     */
    @NotNull(message = "Không được để trống trạng thái hàng.")
    StockStatus statusProduct;

    /**
     * Từ khóa SEO (optional).
     */
    Set<String> keywords;

    /**
     * Sku của Product chứa variant này (bắt buộc).
     */
    @JsonProperty("product_sku")
    @JsonAlias({ "productSku", "product_sku" })
    @NotNull(message = "Không được để trống nguồn mã sản phẩm")
    String productSku;

    /**
     * Thông số kỹ thuật (optional).
     */
    List<SpecificationGroupDto> specifications;

    /**
     * Ưu đãi (promotion).
     */
    List<PromotionDto> promotions;

    /**
     * Các nhóm variant option để tạo tổ hợp (bắt buộc, ít nhất 1).
     * VD: [ {key:"Color", values:["Đen","Trắng"]}, {key:"Storage",
     * values:["128GB","256GB"]} ]
     * → Tạo 4 variants: Đen-128GB, Đen-256GB, Trắng-128GB, Trắng-256GB
     */
    @NotEmpty(message = "Phải có ít nhất 1 variant group")
    @Valid
    List<VariantGroupInput> variantGroups;
}
