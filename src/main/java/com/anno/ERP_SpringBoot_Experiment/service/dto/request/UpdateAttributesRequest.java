package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request DTO để cập nhật Attributes (variant).
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateAttributesRequest {

    /**
     * SKU của Attributes cần cập nhật (bắt buộc).
     */
    @JsonProperty("sku")
    String sku;

    /**
     * Tên mới (optional).
     */
    @JsonProperty("name")
    String name;

    /**
     * Giá mới (optional).
     */
    @JsonProperty("price")
    Double price;

    /**
     * Giá khuyến mãi mới (optional).
     */
    @JsonProperty("sale_price")
    Double salePrice;

    /**
     * Màu sắc mới (bắt buộc).
     */
    @NotBlank(message = "Không được để trống mới.")
    String color;

    /**
     * Option mới như size, dung lượng (bắt buộc).
     */
    @NotBlank(message = "Không được để trống option.")
    String option;

    /**
     * Số lượng tồn kho mới (optional).
     */
    @JsonProperty("stock_quantity")
    Integer stockQuantity;

    /**
     * Từ khóa SEO mới (optional).
     */
    @JsonProperty("keywords")
    List<String> keywords;

    /**
     * Dữ liệu specifications mới (optional).
     */
    @JsonProperty("data")
    List<String> data;
}
