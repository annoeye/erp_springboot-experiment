package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.common.annotation.NormalizedId;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO để cập nhật thông tin Product.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProductRequest {

    /**
     * ID của Product cần cập nhật (bắt buộc).
     * Được normalize tự động: uppercase + remove dashes.
     */
    @NormalizedId
    @NotNull(message = "ID sản phẩm không được để trống")
    String id;

    /**
     * Tên mới của sản phẩm (optional).
     */
    String name;

    /**
     * Id của Category mới (optional).
     */
    @NormalizedId
    String categoryId;

    /**
     * Trạng thái active của sản phẩm (optional).
     */
    ActiveStatus status;
}
