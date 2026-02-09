package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.common.annotation.NormalizedId;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PromotionDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.SpecificationDto;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
 * Tự động tạo tổ hợp từ danh sách colors × options.
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
     * ID của Product chứa variant này (bắt buộc).
     * Hỗ trợ nhiều format: id, productId, product_id.
     * Được normalize tự động: uppercase + remove dashes.
     */
    @NormalizedId
    @JsonProperty("id")
    @JsonAlias({ "productId", "product_id" })
    @NotNull(message = "Không được để trống nguồn sản phẩm")
    String productId;

    /**
     * Thông số kỹ thuật (optional).
     */
    List<SpecificationDto> specifications;

    /**
     * Ưu đãi (promotion).
     */
    List<PromotionDto> promotions;

    /**
     * Danh sách màu sắc để tạo tổ hợp (bắt buộc, ít nhất 1).
     */
    @NotEmpty(message = "Phải có ít nhất 1 color")
    List<String> colors;

    /**
     * Danh sách option (size, dung lượng...) để tạo tổ hợp (bắt buộc, ít nhất 1).
     */
    @NotEmpty(message = "Phải có ít nhất 1 option")
    List<String> options;
}
