package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.common.annotation.NormalizedId;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO để tạo Product mới.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateProductRequest {

    /**
     * Tên sản phẩm (bắt buộc).
     */
    @JsonProperty("name")
    @NotBlank(message = "Tên sản phẩm không được để trống.")
    String name;

    /**
     * ID của Category chứa sản phẩm (bắt buộc).
     * Hỗ trợ nhiều format: id, categoryId, category_id.
     * Được normalize tự động: uppercase + remove dashes.
     */
    @NormalizedId
    @JsonProperty("id")
    @JsonAlias({ "categoryId", "category_id" })
    @NotNull(message = "ID danh mục không được để trống.")
    String categoryId;
}
